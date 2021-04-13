import RSocketWebSocketClient from "rsocket-websocket-client";
import {BufferEncoders, RSocketClient, MESSAGE_RSOCKET_COMPOSITE_METADATA, MESSAGE_RSOCKET_ROUTING, encodeCompositeMetadata, encodeRoute} from "rsocket-core";

export async function connect(userId) {
    console.log("connecting")
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
            payload: {
                metadata: encodeCompositeMetadata([
                    [MESSAGE_RSOCKET_ROUTING, encodeRoute('game.players.login')],
                ]),
                data: Buffer.from(userId)
            }
        },
        responders: {
            requestChannel: flowable => flowable
        },
        transport: wsClient,
    });
    const rsocket = await socketClient.connect();
    return [wsClient, rsocket];
}
