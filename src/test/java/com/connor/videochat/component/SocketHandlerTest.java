package com.connor.videochat.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class SocketHandlerTest {

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    @Mock
    private WebSocketSession session3;

    @Mock
    private TextMessage message;

    private SocketHandler socketHandler;

    @BeforeEach
    public void setup() throws Exception {
        try(AutoCloseable openMocks = MockitoAnnotations.openMocks(this)){
            socketHandler = new SocketHandler();
            socketHandler.sessions = new ArrayList<>();
            
        }
    }

    @Test
    public void testSendToAllOpenSessionsExceptSender() throws Exception {
        socketHandler.sessions.add(session1);
        socketHandler.sessions.add(session2);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        when(session1.getId()).thenReturn("session1");
        when(session2.getId()).thenReturn("session2");
        
        socketHandler.handleTextMessage(session1, message);
        
        verify(session2, times(1)).sendMessage(message);
        verify(session1, never()).sendMessage(message);
    }

    @Test
    public void testSendToNoSessionsWhenAllClosed() throws Exception {
        
        socketHandler.sessions.add(session1);
        socketHandler.sessions.add(session2);
        when(session1.isOpen()).thenReturn(false);
        when(session2.isOpen()).thenReturn(false);
        
        socketHandler.handleTextMessage(session1, message);

        verify(session1, never()).sendMessage(message);
        verify(session2, never()).sendMessage(message);
    }

    @Test
    public void testSendToNoSessionsWhenOnlyOneOpen() throws Exception {
        socketHandler.sessions.add(session1);
        when(session1.isOpen()).thenReturn(true);
        when(session1.getId()).thenReturn("session1");

        socketHandler.handleTextMessage(session1, message);

        verify(session1, never()).sendMessage(message);
    }

    @Test
    public void testSendToMultipleOpenSessions() throws Exception {
        socketHandler.sessions.add(session1);
        socketHandler.sessions.add(session2);
        socketHandler.sessions.add(session3);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        when(session3.isOpen()).thenReturn(true);
        when(session1.getId()).thenReturn("session1");
        when(session2.getId()).thenReturn("session2");
        when(session3.getId()).thenReturn("session3");

        socketHandler.handleTextMessage(session1, message);

        verify(session2, times(1)).sendMessage(message);
        verify(session3, times(1)).sendMessage(message);
        verify(session1, never()).sendMessage(message);
    }

    @Test
    public void testInterruptedException() throws Exception {
        socketHandler.sessions.add(session1);
        socketHandler.sessions.add(session2);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
        when(session1.getId()).thenReturn("session1");
        when(session2.getId()).thenReturn("session2");
        doThrow(new IOException("TestException")).when(session2).sendMessage(message);

        IOException exception = assertThrows(
                IOException.class,
                () -> socketHandler.handleTextMessage(session1, message),
                "Expected handleTextMessage to throw IOException, but it didn't"
        );

        assertTrue(exception.getMessage().contains("TestException"));
    }

    @Test
    public void testAfterConnectionEstablished() {
        socketHandler.afterConnectionEstablished(session1);
        assertTrue(socketHandler.sessions.contains(session1));
    }
}