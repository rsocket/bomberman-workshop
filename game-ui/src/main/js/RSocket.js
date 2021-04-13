import RSocketWebSocketClient from "rsocket-websocket-client";
import {BufferEncoders, RSocketClient, MESSAGE_RSOCKET_COMPOSITE_METADATA, MESSAGE_RSOCKET_ROUTING, encodeCompositeMetadata, encodeRoute} from "rsocket-core";

export async function connect(userId, responder) {
    if (window.wsClient && window.rsocket) {
        return [window.wsClient, window.rsocket];
    }
    console.log("connecting");
    const port = window.location.port ? `:${window.location.port}` : "";
    const isSecure = window.location.protocol === 'https:';
    const hostname = window.location.hostname;
    const wsClient = new RSocketWebSocketClient({
        url: `${isSecure ? 'wss' : 'ws'}://${hostname}${port}/rsocket`
    }, BufferEncoders);
    const socketClient = new RSocketClient({
        setup: {
            keepAlive: 30000,
            lifetime: 90000,
            dataMimeType: 'application/octet-stream',
            metadataMimeType: MESSAGE_RSOCKET_COMPOSITE_METADATA.string,
        },
        responder: responder,
        transport: wsClient,
    });
    const rsocket = await socketClient.connect();
    // not proud of this one
    window.wsClient = wsClient;
    window.rsocket = rsocket;
    return [wsClient, rsocket];
}
