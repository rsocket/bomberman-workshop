import {flatbuffers} from "flatbuffers";
import {xyz} from "generated/flatbuffers/RoomEvent_generated";

const RoomEvent = xyz.bomberman.room.data.RoomEvent;
const EventType = xyz.bomberman.room.data.EventType;

export function extractRoom(roomEventBuffer) {
  const dataBuf = flatbuffers.ByteBuffer.allocate(roomEventBuffer.data);
  const event = RoomEvent.getRootAsRoomEvent(dataBuf);
  const eventType = event.type() === EventType.Added ? "Added" : event.type() === EventType.Updated ? "Updated" : "Removed";
  const roomId = event.id();
  const players = [...new Array(event.playersLength()).keys()]
  .map(i => {
    const player = event.players(i);
    return {
      id: player.id(),
      name: player.name(),
    };
  })
  let playerOwner = event.owner();
  const owner = {
    id: playerOwner.id(),
    name: playerOwner.name(),
  };
  const room = {
    id: roomId,
    owner: owner,
    players: players
  };
  return {eventType, room};
}