import RSocketWebSocketClient from "rsocket-websocket-client";
import {IdentitySerializer, JsonSerializer, RSocketClient} from "rsocket-core";

export async function connect() {
    console.log("connecting")
    const port = window.location.port ? `:${window.location.port}` : "";
    const isSecure = window.location.protocol === 'https:';
    const hostname = window.location.hostname;
    const wsClient = new RSocketWebSocketClient({
        url: `${isSecure ? 'wss' : 'ws'}://${hostname}${port}/rsocket`
    });
    const socketClient = new RSocketClient({
        serializers: {
            data: JsonSerializer,
            metadata: IdentitySerializer
        },
        setup: {
            keepAlive: 30000,
            lifetime: 90000,
            dataMimeType: 'application/json',
            metadataMimeType: 'message/x.rsocket.routing.v0',
        },
        transport: wsClient,
    });
    const rsocket = await socketClient.connect();
    return [wsClient, rsocket];
}
