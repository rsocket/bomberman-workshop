// automatically generated by the FlatBuffers compiler, do not modify

/**
 * @const
 * @namespace
 */
var xyz = xyz || {};

/**
 * @const
 * @namespace
 */
xyz.bomberman = xyz.bomberman || {};

/**
 * @const
 * @namespace
 */
xyz.bomberman.room = xyz.bomberman.room || {};

/**
 * @const
 * @namespace
 */
xyz.bomberman.room.data = xyz.bomberman.room.data || {};

/**
 * @enum {number}
 */
xyz.bomberman.room.data.EventType = {
  Added: 0,
  Removed: 1
};

/**
 * @enum {string}
 */
xyz.bomberman.room.data.EventTypeName = {
  '0': 'Added',
  '1': 'Removed'
};

/**
 * @constructor
 */
xyz.bomberman.room.data.RoomEvent = function() {
  /**
   * @type {flatbuffers.ByteBuffer}
   */
  this.bb = null;

  /**
   * @type {number}
   */
  this.bb_pos = 0;
};

/**
 * @param {number} i
 * @param {flatbuffers.ByteBuffer} bb
 * @returns {xyz.bomberman.room.data.RoomEvent}
 */
xyz.bomberman.room.data.RoomEvent.prototype.__init = function(i, bb) {
  this.bb_pos = i;
  this.bb = bb;
  return this;
};

/**
 * @param {flatbuffers.ByteBuffer} bb
 * @param {xyz.bomberman.room.data.RoomEvent=} obj
 * @returns {xyz.bomberman.room.data.RoomEvent}
 */
xyz.bomberman.room.data.RoomEvent.getRootAsRoomEvent = function(bb, obj) {
  return (obj || new xyz.bomberman.room.data.RoomEvent).__init(bb.readInt32(bb.position()) + bb.position(), bb);
};

/**
 * @param {flatbuffers.ByteBuffer} bb
 * @param {xyz.bomberman.room.data.RoomEvent=} obj
 * @returns {xyz.bomberman.room.data.RoomEvent}
 */
xyz.bomberman.room.data.RoomEvent.getSizePrefixedRootAsRoomEvent = function(bb, obj) {
  bb.setPosition(bb.position() + flatbuffers.SIZE_PREFIX_LENGTH);
  return (obj || new xyz.bomberman.room.data.RoomEvent).__init(bb.readInt32(bb.position()) + bb.position(), bb);
};

/**
 * @param {flatbuffers.Encoding=} optionalEncoding
 * @returns {string|Uint8Array|null}
 */
xyz.bomberman.room.data.RoomEvent.prototype.id = function(optionalEncoding) {
  var offset = this.bb.__offset(this.bb_pos, 4);
  return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
};

/**
 * @returns {xyz.bomberman.room.data.EventType}
 */
xyz.bomberman.room.data.RoomEvent.prototype.type = function() {
  var offset = this.bb.__offset(this.bb_pos, 6);
  return offset ? /** @type {xyz.bomberman.room.data.EventType} */ (this.bb.readInt8(this.bb_pos + offset)) : xyz.bomberman.room.data.EventType.Added;
};

/**
 * @param {xyz.bomberman.room.data.EventType} value
 * @returns {boolean}
 */
xyz.bomberman.room.data.RoomEvent.prototype.mutate_type = function(value) {
  var offset = this.bb.__offset(this.bb_pos, 6);

  if (offset === 0) {
    return false;
  }

  this.bb.writeInt8(this.bb_pos + offset, value);
  return true;
};

/**
 * @param {flatbuffers.Builder} builder
 */
xyz.bomberman.room.data.RoomEvent.startRoomEvent = function(builder) {
  builder.startObject(2);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} idOffset
 */
xyz.bomberman.room.data.RoomEvent.addId = function(builder, idOffset) {
  builder.addFieldOffset(0, idOffset, 0);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {xyz.bomberman.room.data.EventType} type
 */
xyz.bomberman.room.data.RoomEvent.addType = function(builder, type) {
  builder.addFieldInt8(1, type, xyz.bomberman.room.data.EventType.Added);
};

/**
 * @param {flatbuffers.Builder} builder
 * @returns {flatbuffers.Offset}
 */
xyz.bomberman.room.data.RoomEvent.endRoomEvent = function(builder) {
  var offset = builder.endObject();
  return offset;
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} offset
 */
xyz.bomberman.room.data.RoomEvent.finishRoomEventBuffer = function(builder, offset) {
  builder.finish(offset);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} offset
 */
xyz.bomberman.room.data.RoomEvent.finishSizePrefixedRoomEventBuffer = function(builder, offset) {
  builder.finish(offset, undefined, true);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} idOffset
 * @param {xyz.bomberman.room.data.EventType} type
 * @returns {flatbuffers.Offset}
 */
xyz.bomberman.room.data.RoomEvent.createRoomEvent = function(builder, idOffset, type) {
  xyz.bomberman.room.data.RoomEvent.startRoomEvent(builder);
  xyz.bomberman.room.data.RoomEvent.addId(builder, idOffset);
  xyz.bomberman.room.data.RoomEvent.addType(builder, type);
  return xyz.bomberman.room.data.RoomEvent.endRoomEvent(builder);
}

// Exports for Node.js and RequireJS
this.xyz = xyz;
