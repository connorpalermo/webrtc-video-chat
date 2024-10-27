package com.connor.videochat.config;

import com.connor.videochat.component.SocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class WebSocketConfigurationTest {

    private WebSocketConfiguration webSocketConfiguration;
    private WebSocketHandlerRegistry registry;
    private WebSocketHandlerRegistration registration;

    @BeforeEach
    void setUp() {
        webSocketConfiguration = new WebSocketConfiguration();
        registry = mock(WebSocketHandlerRegistry.class);
        registration = mock(WebSocketHandlerRegistration.class);
    }

    @Test
    void testRegisterWebSocketHandlers() {
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);

        when(registry.addHandler(any(SocketHandler.class), pathCaptor.capture())).thenReturn(registration);
        when(registration.setAllowedOrigins(anyString())).thenReturn(registration);

        webSocketConfiguration.registerWebSocketHandlers(registry);

        verify(registry, times(2)).addHandler(any(SocketHandler.class), anyString());

        assertEquals("/socket1", pathCaptor.getAllValues().get(0));
        assertEquals("/socket2", pathCaptor.getAllValues().get(1));

        verify(registration, times(2)).setAllowedOrigins("*");
    }
}
