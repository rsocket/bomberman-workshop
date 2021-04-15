import RSocketWebSocketClient from "rsocket-websocket-client";
import {BufferEncoders, MESSAGE_RSOCKET_COMPOSITE_METADATA, RSocketClient} from "rsocket-core";

export function connect(responder) {
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
    return socketClient.connect();
}
