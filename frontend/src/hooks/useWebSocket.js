import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = process.env.REACT_APP_WS_URL || 'https://ai-mock-interview-g9dz.onrender.com/ws';

export function useWebSocket(sessionId, onFeedback) {
  const clientRef = useRef(null);

  useEffect(() => {
    if (!sessionId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3000,
      onConnect: () => {
        console.log('WS connected');
        // Subscribe to feedback for this session
        client.subscribe(`/topic/feedback/${sessionId}`, (msg) => {
          try {
            const data = JSON.parse(msg.body);
            onFeedback?.(data);
          } catch (e) {
            console.error('WS parse error', e);
          }
        });
      },
      onDisconnect: () => console.log('WS disconnected'),
      onStompError: (err) => console.error('WS STOMP error', err),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [sessionId]);

  const sendTyping = useCallback(() => {
    if (clientRef.current?.connected) {
      clientRef.current.publish({
        destination: '/app/typing',
        body: JSON.stringify({ sessionId, typing: true }),
      });
    }
  }, [sessionId]);

  return { sendTyping };
}
