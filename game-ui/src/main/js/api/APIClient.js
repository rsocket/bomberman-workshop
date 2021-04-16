import {
  encodeCompositeMetadata,
  encodeRoute,
  MESSAGE_RSOCKET_ROUTING
} from "rsocket-core";
import {extractRoom} from "./flatbuffers/Mapper";
import {connect} from "./RSocket";

export default class APIClient {
  constructor(rsocket) {
    this.rsocket = rsocket;
  }

  login(userName) {
    return new Promise((resolve, reject) =>
        this.rsocket.requestResponse({
          metadata: encodeCompositeMetadata([
            [MESSAGE_RSOCKET_ROUTING, encodeRoute('game.players.login')],
          ]),
          data: Buffer.from(userName)
        }).subscribe({
          onComplete: (payload) => {
            resolve(payload.data.toString());
          },
          onError: e => reject(e)
        })
    );
  }

  get rooms() {
    const rsocket = this.rsocket;
    return {
      listAndListen: (roomsHandler) => {
        return new Promise((resolve, reject) =>
            rsocket.requestStream({
              metadata: encodeCompositeMetadata([
                [MESSAGE_RSOCKET_ROUTING, encodeRoute('game.rooms')],
              ]),
            }).subscribe({
              onSubscribe(s) {
                s.request(2147483642)
              },
              onNext(eventBuf) {
                roomsHandler(extractRoom(eventBuf));
              },
              onError(err) {
                reject(err)
              },
              onComplete() {
                resolve()
              }
            })
        );
      },

      create: () => new Promise((resolve, reject) =>
          rsocket.requestResponse({
            metadata: encodeCompositeMetadata([
              [MESSAGE_RSOCKET_ROUTING, encodeRoute('game.rooms.create')],
            ]),
          })
          .then(payload => resolve(payload.data.toString()), reject)
      ),

      join: (roomId) => new Promise((resolve, reject) =>
          rsocket.requestResponse({
            metadata: encodeCompositeMetadata([
              [MESSAGE_RSOCKET_ROUTING,
                encodeRoute(`game.rooms.${roomId}.join`)],
            ]),
          })
          .then(resolve, reject)
      ),

      leave: (roomId) => new Promise((resolve, reject) =>
          rsocket.requestResponse({
            metadata: encodeCompositeMetadata([
              [MESSAGE_RSOCKET_ROUTING,
                encodeRoute(`game.rooms.${roomId}.leave`)],
            ]),
          })
          .then(resolve, reject)
      ),

      start: (roomId) => rsocket.fireAndForget({
        metadata: encodeCompositeMetadata([
          [MESSAGE_RSOCKET_ROUTING, encodeRoute(`game.rooms.${roomId}.start`)],
        ]),
      }),

      close: (roomId) => new Promise((resolve, reject) =>
          rsocket.requestResponse({
            metadata: encodeCompositeMetadata([
              [MESSAGE_RSOCKET_ROUTING,
                encodeRoute(`game.rooms.${roomId}.close`)],
            ]),
          })
          .then(resolve, reject)
      ),
    }
  }

}

APIClient.create = function (onGameStartedHandler) {
  return connect({
    requestChannel: flowable => onGameStartedHandler(flowable)
  })
  .then((rsocket) => new APIClient(rsocket))
}
