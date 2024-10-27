document.addEventListener('DOMContentLoaded', function() {
    var conn;
    conn = new WebSocket('ws://localhost:8080/socket');

    // Get the local and remote video elements
    var localVideo = document.getElementById('localVideo');
    var remoteVideo = document.getElementById('remoteVideo');

    // Create a new peer connection
    var pc = new RTCPeerConnection();

    console.log('Peer connection object:', pc);

    // Set up the peer connection event handlers
    pc.ontrack = function(event) {
        console.log('Got remote track');
        if (remoteVideo.srcObject !== event.streams[0]) {
            remoteVideo.srcObject = event.streams[0];
            remoteVideo.play();
        }
    };

    pc.onicecandidate = function(event) {
        if (event.candidate) {
            console.log('Sending ICE candidate');
            conn.send(JSON.stringify({
                type: 'candidate',
                candidate: event.candidate
            }));
        }
    };

    // Set up the signaling server connection
    conn.onopen = function() {
        console.log('Connected to the signaling server');

        // Request access to the user's camera and microphone
        navigator.mediaDevices.getUserMedia({ audio: true, video: true })
            .then(function(stream) {
            console.log('Got user media');
            localVideo.srcObject = stream;
            stream.getTracks().forEach(function(track) {
                pc.addTrack(track, stream);
            });

            // Create an offer and send it to the other peer
            pc.createOffer()
                .then(function(offer) {
                console.log('Created offer:', offer);
                return pc.setLocalDescription(offer);
            })
                .then(function() {
                console.log('Sent offer to other peer');
                conn.send(JSON.stringify({
                    type: 'offer',
                    sdp: pc.localDescription
                }));
            })
                .catch(function(error) {
                console.error('Error creating or sending offer:', error);
            });
        })
            .catch(function(error) {
            console.error('Error getting user media:', error);
        });
    };

    conn.onmessage = function(event) {
        console.log('Received message from signaling server:', event.data);
        var message = JSON.parse(event.data);
        console.log('Message type:', message.type);

        switch (message.type) {
            case 'offer':
                pc.setRemoteDescription(new RTCSessionDescription(message.sdp))
                    .then(function() {
                    console.log('Remote description set');
                    return pc.createAnswer();
                })
                    .then(function(answer) {
                    console.log('Created answer:', answer);
                    return pc.setLocalDescription(answer);
                })
                    .then(function() {
                    conn.send(JSON.stringify({
                        type: 'answer',
                        sdp: pc.localDescription
                    }));
                })
                    .catch(function(error) {
                    console.error('Error handling offer:', error);
                });
                break;
            case 'answer':
                pc.setRemoteDescription(new RTCSessionDescription(message.sdp))
                    .then(function() {
                    console.log('Remote description set for answer');
                })
                    .catch(function(error) {
                    console.error('Error setting remote description for answer:', error);
                });
                break;
            case 'candidate':
                pc.addIceCandidate(new RTCIceCandidate(message.candidate))
                    .then(function() {
                    console.log('Added ICE candidate');
                })
                    .catch(function(error) {
                    console.error('Error adding received ICE candidate:', error);
                });
                break;
            default:
                console.error('Unknown message type:', message.type);
        }
    };

    conn.onerror = function(event) {
        console.error('Error occurred:', event);
    };

    conn.onclose = function() {
        console.log('Connection to signaling server closed');
    };
});
