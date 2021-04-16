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
    return Promise.reject("not implemented")
  }

  get rooms() {
    const rsocket = this.rsocket;
    return {
      listAndListen: (roomsHandler) => {
        return Promise.reject("not implemented")
      },

      create: () => Promise.reject("not implemented"),

      join: (roomId) => Promise.reject("not implemented"),

      leave: (roomId) => Promise.reject("not implemented"),

      start: (roomId) => {},

      close: (roomId) => {},
    }
  }

}

APIClient.create = function (onGameStartedHandler) {
  return connect({
    requestChannel: flowable => onGameStartedHandler(flowable)
  })
  .then((rsocket) => new APIClient(rsocket))
}