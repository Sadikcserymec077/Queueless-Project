import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { SOCKET_BASE_URL, storage } from "./api.js";

export function subscribeToCounter(counterId, onMessage) {
  if (!counterId) {
    return () => {};
  }

  const client = new Client({
    webSocketFactory: () => new SockJS(`${SOCKET_BASE_URL}/ws`),
    reconnectDelay: 5000,
    connectHeaders: storage.getToken() ? { Authorization: `Bearer ${storage.getToken()}` } : {},
    onConnect: () => {
      client.subscribe(`/topic/counters/${counterId}`, (message) => {
        onMessage(JSON.parse(message.body));
      });
    }
  });

  client.activate();
  return () => client.deactivate();
}
