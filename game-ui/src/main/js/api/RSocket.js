import RSocketWebSocketClient from "rsocket-websocket-client";
import {
    BufferEncoders,
    MESSAGE_RSOCKET_COMPOSITE_METADATA,
    RSocketClient
} from "rsocket-core";

function urlFromLocation() {
    const port = window.location.port ? `:${window.location.port}` : "";
    const isSecure = window.location.protocol === 'https:';
    const hostname = window.location.hostname;
    return `${isSecure ? 'wss' : 'ws'}://${hostname}${port}/rsocket`;
}

export async function connect(responder) {
    console.log("connecting");
    const socketClient = new RSocketClient({
        setup: {
            keepAlive: 30000,
            lifetime: 90000,
            dataMimeType: 'application/octet-stream',
            metadataMimeType: MESSAGE_RSOCKET_COMPOSITE_METADATA.string,
        },
        responder: responder,
        transport: new RSocketWebSocketClient({url : urlFromLocation()}, BufferEncoders),
    });
    return await socketClient.connect();
}
