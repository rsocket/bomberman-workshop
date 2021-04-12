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
xyz.bomberman.discovery = xyz.bomberman.discovery || {};

/**
 * @const
 * @namespace
 */
xyz.bomberman.discovery.data = xyz.bomberman.discovery.data || {};

/**
 * @enum {number}
 */
xyz.bomberman.discovery.data.TransportType = {
  Tcp: 0,
  WebSocket: 1
};

/**
 * @enum {string}
 */
xyz.bomberman.discovery.data.TransportTypeName = {
  '0': 'Tcp',
  '1': 'WebSocket'
};

/**
 * @enum {number}
 */
xyz.bomberman.discovery.data.AnyServiceEvent = {
  NONE: 0,
  Connected: 1,
  Disconnected: 2
};

/**
 * @enum {string}
 */
xyz.bomberman.discovery.data.AnyServiceEventName = {
  '0': 'NONE',
  '1': 'Connected',
  '2': 'Disconnected'
};

/**
 * @constructor
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent = function() {
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
 * @returns {xyz.bomberman.discovery.data.ServiceConnectedEvent}
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.prototype.__init = function(i, bb) {
  this.bb_pos = i;
  this.bb = bb;
  return this;
};

/**
 * @param {flatbuffers.ByteBuffer} bb
 * @param {xyz.bomberman.discovery.data.ServiceConnectedEvent=} obj
 * @returns {xyz.bomberman.discovery.data.ServiceConnectedEvent}
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.getRootAsServiceConnectedEvent = function(bb, obj) {
  return (obj || new xyz.bomberman.discovery.data.ServiceConnectedEvent).__init(bb.readInt32(bb.position()) + bb.position(), bb);
};

/**
 * @param {flatbuffers.ByteBuffer} bb
 * @param {xyz.bomberman.discovery.data.ServiceConnectedEvent=} obj
 * @returns {xyz.bomberman.discovery.data.ServiceConnectedEvent}
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.getSizePrefixedRootAsServiceConnectedEvent = function(bb, obj) {
  bb.setPosition(bb.position() + flatbuffers.SIZE_PREFIX_LENGTH);
  return (obj || new xyz.bomberman.discovery.data.ServiceConnectedEvent).__init(bb.readInt32(bb.position()) + bb.position(), bb);
};

/**
 * @param {flatbuffers.Encoding=} optionalEncoding
 * @returns {string|Uint8Array|null}
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.prototype.id = function(optionalEncoding) {
  var offset = this.bb.__offset(this.bb_pos, 4);
  return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
};

/**
 * @param {flatbuffers.Encoding=} optionalEncoding
 * @returns {string|Uint8Array|null}
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.prototype.host = function(optionalEncoding) {
  var offset = this.bb.__offset(this.bb_pos, 6);
  return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
};

/**
 * @returns {number}
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.prototype.port = function() {
  var offset = this.bb.__offset(this.bb_pos, 8);
  return offset ? this.bb.readInt32(this.bb_pos + offset) : 0;
};

/**
 * @param {number} value
 * @returns {boolean}
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.prototype.mutate_port = function(value) {
  var offset = this.bb.__offset(this.bb_pos, 8);

  if (offset === 0) {
    return false;
  }

  this.bb.writeInt32(this.bb_pos + offset, value);
  return true;
};

/**
 * @returns {xyz.bomberman.discovery.data.TransportType}
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.prototype.transport = function() {
  var offset = this.bb.__offset(this.bb_pos, 10);
  return offset ? /** @type {xyz.bomberman.discovery.data.TransportType} */ (this.bb.readInt8(this.bb_pos + offset)) : xyz.bomberman.discovery.data.TransportType.Tcp;
};

/**
 * @param {xyz.bomberman.discovery.data.TransportType} value
 * @returns {boolean}
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.prototype.mutate_transport = function(value) {
  var offset = this.bb.__offset(this.bb_pos, 10);

  if (offset === 0) {
    return false;
  }

  this.bb.writeInt8(this.bb_pos + offset, value);
  return true;
};

/**
 * @param {flatbuffers.Builder} builder
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.startServiceConnectedEvent = function(builder) {
  builder.startObject(4);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} idOffset
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.addId = function(builder, idOffset) {
  builder.addFieldOffset(0, idOffset, 0);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} hostOffset
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.addHost = function(builder, hostOffset) {
  builder.addFieldOffset(1, hostOffset, 0);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {number} port
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.addPort = function(builder, port) {
  builder.addFieldInt32(2, port, 0);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {xyz.bomberman.discovery.data.TransportType} transport
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.addTransport = function(builder, transport) {
  builder.addFieldInt8(3, transport, xyz.bomberman.discovery.data.TransportType.Tcp);
};

/**
 * @param {flatbuffers.Builder} builder
 * @returns {flatbuffers.Offset}
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.endServiceConnectedEvent = function(builder) {
  var offset = builder.endObject();
  return offset;
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} idOffset
 * @param {flatbuffers.Offset} hostOffset
 * @param {number} port
 * @param {xyz.bomberman.discovery.data.TransportType} transport
 * @returns {flatbuffers.Offset}
 */
xyz.bomberman.discovery.data.ServiceConnectedEvent.createServiceConnectedEvent = function(builder, idOffset, hostOffset, port, transport) {
  xyz.bomberman.discovery.data.ServiceConnectedEvent.startServiceConnectedEvent(builder);
  xyz.bomberman.discovery.data.ServiceConnectedEvent.addId(builder, idOffset);
  xyz.bomberman.discovery.data.ServiceConnectedEvent.addHost(builder, hostOffset);
  xyz.bomberman.discovery.data.ServiceConnectedEvent.addPort(builder, port);
  xyz.bomberman.discovery.data.ServiceConnectedEvent.addTransport(builder, transport);
  return xyz.bomberman.discovery.data.ServiceConnectedEvent.endServiceConnectedEvent(builder);
}

/**
 * @constructor
 */
xyz.bomberman.discovery.data.ServiceDisconnectedEvent = function() {
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
 * @returns {xyz.bomberman.discovery.data.ServiceDisconnectedEvent}
 */
xyz.bomberman.discovery.data.ServiceDisconnectedEvent.prototype.__init = function(i, bb) {
  this.bb_pos = i;
  this.bb = bb;
  return this;
};

/**
 * @param {flatbuffers.ByteBuffer} bb
 * @param {xyz.bomberman.discovery.data.ServiceDisconnectedEvent=} obj
 * @returns {xyz.bomberman.discovery.data.ServiceDisconnectedEvent}
 */
xyz.bomberman.discovery.data.ServiceDisconnectedEvent.getRootAsServiceDisconnectedEvent = function(bb, obj) {
  return (obj || new xyz.bomberman.discovery.data.ServiceDisconnectedEvent).__init(bb.readInt32(bb.position()) + bb.position(), bb);
};

/**
 * @param {flatbuffers.ByteBuffer} bb
 * @param {xyz.bomberman.discovery.data.ServiceDisconnectedEvent=} obj
 * @returns {xyz.bomberman.discovery.data.ServiceDisconnectedEvent}
 */
xyz.bomberman.discovery.data.ServiceDisconnectedEvent.getSizePrefixedRootAsServiceDisconnectedEvent = function(bb, obj) {
  bb.setPosition(bb.position() + flatbuffers.SIZE_PREFIX_LENGTH);
  return (obj || new xyz.bomberman.discovery.data.ServiceDisconnectedEvent).__init(bb.readInt32(bb.position()) + bb.position(), bb);
};

/**
 * @param {flatbuffers.Encoding=} optionalEncoding
 * @returns {string|Uint8Array|null}
 */
xyz.bomberman.discovery.data.ServiceDisconnectedEvent.prototype.id = function(optionalEncoding) {
  var offset = this.bb.__offset(this.bb_pos, 4);
  return offset ? this.bb.__string(this.bb_pos + offset, optionalEncoding) : null;
};

/**
 * @param {flatbuffers.Builder} builder
 */
xyz.bomberman.discovery.data.ServiceDisconnectedEvent.startServiceDisconnectedEvent = function(builder) {
  builder.startObject(1);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} idOffset
 */
xyz.bomberman.discovery.data.ServiceDisconnectedEvent.addId = function(builder, idOffset) {
  builder.addFieldOffset(0, idOffset, 0);
};

/**
 * @param {flatbuffers.Builder} builder
 * @returns {flatbuffers.Offset}
 */
xyz.bomberman.discovery.data.ServiceDisconnectedEvent.endServiceDisconnectedEvent = function(builder) {
  var offset = builder.endObject();
  return offset;
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} idOffset
 * @returns {flatbuffers.Offset}
 */
xyz.bomberman.discovery.data.ServiceDisconnectedEvent.createServiceDisconnectedEvent = function(builder, idOffset) {
  xyz.bomberman.discovery.data.ServiceDisconnectedEvent.startServiceDisconnectedEvent(builder);
  xyz.bomberman.discovery.data.ServiceDisconnectedEvent.addId(builder, idOffset);
  return xyz.bomberman.discovery.data.ServiceDisconnectedEvent.endServiceDisconnectedEvent(builder);
}

/**
 * @constructor
 */
xyz.bomberman.discovery.data.ServiceEvent = function() {
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
 * @returns {xyz.bomberman.discovery.data.ServiceEvent}
 */
xyz.bomberman.discovery.data.ServiceEvent.prototype.__init = function(i, bb) {
  this.bb_pos = i;
  this.bb = bb;
  return this;
};

/**
 * @param {flatbuffers.ByteBuffer} bb
 * @param {xyz.bomberman.discovery.data.ServiceEvent=} obj
 * @returns {xyz.bomberman.discovery.data.ServiceEvent}
 */
xyz.bomberman.discovery.data.ServiceEvent.getRootAsServiceEvent = function(bb, obj) {
  return (obj || new xyz.bomberman.discovery.data.ServiceEvent).__init(bb.readInt32(bb.position()) + bb.position(), bb);
};

/**
 * @param {flatbuffers.ByteBuffer} bb
 * @param {xyz.bomberman.discovery.data.ServiceEvent=} obj
 * @returns {xyz.bomberman.discovery.data.ServiceEvent}
 */
xyz.bomberman.discovery.data.ServiceEvent.getSizePrefixedRootAsServiceEvent = function(bb, obj) {
  bb.setPosition(bb.position() + flatbuffers.SIZE_PREFIX_LENGTH);
  return (obj || new xyz.bomberman.discovery.data.ServiceEvent).__init(bb.readInt32(bb.position()) + bb.position(), bb);
};

/**
 * @returns {xyz.bomberman.discovery.data.AnyServiceEvent}
 */
xyz.bomberman.discovery.data.ServiceEvent.prototype.eventType = function() {
  var offset = this.bb.__offset(this.bb_pos, 4);
  return offset ? /** @type {xyz.bomberman.discovery.data.AnyServiceEvent} */ (this.bb.readUint8(this.bb_pos + offset)) : xyz.bomberman.discovery.data.AnyServiceEvent.NONE;
};

/**
 * @param {flatbuffers.Table} obj
 * @returns {?flatbuffers.Table}
 */
xyz.bomberman.discovery.data.ServiceEvent.prototype.event = function(obj) {
  var offset = this.bb.__offset(this.bb_pos, 6);
  return offset ? this.bb.__union(obj, this.bb_pos + offset) : null;
};

/**
 * @param {flatbuffers.Builder} builder
 */
xyz.bomberman.discovery.data.ServiceEvent.startServiceEvent = function(builder) {
  builder.startObject(2);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {xyz.bomberman.discovery.data.AnyServiceEvent} eventType
 */
xyz.bomberman.discovery.data.ServiceEvent.addEventType = function(builder, eventType) {
  builder.addFieldInt8(0, eventType, xyz.bomberman.discovery.data.AnyServiceEvent.NONE);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} eventOffset
 */
xyz.bomberman.discovery.data.ServiceEvent.addEvent = function(builder, eventOffset) {
  builder.addFieldOffset(1, eventOffset, 0);
};

/**
 * @param {flatbuffers.Builder} builder
 * @returns {flatbuffers.Offset}
 */
xyz.bomberman.discovery.data.ServiceEvent.endServiceEvent = function(builder) {
  var offset = builder.endObject();
  return offset;
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} offset
 */
xyz.bomberman.discovery.data.ServiceEvent.finishServiceEventBuffer = function(builder, offset) {
  builder.finish(offset);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {flatbuffers.Offset} offset
 */
xyz.bomberman.discovery.data.ServiceEvent.finishSizePrefixedServiceEventBuffer = function(builder, offset) {
  builder.finish(offset, undefined, true);
};

/**
 * @param {flatbuffers.Builder} builder
 * @param {xyz.bomberman.discovery.data.AnyServiceEvent} eventType
 * @param {flatbuffers.Offset} eventOffset
 * @returns {flatbuffers.Offset}
 */
xyz.bomberman.discovery.data.ServiceEvent.createServiceEvent = function(builder, eventType, eventOffset) {
  xyz.bomberman.discovery.data.ServiceEvent.startServiceEvent(builder);
  xyz.bomberman.discovery.data.ServiceEvent.addEventType(builder, eventType);
  xyz.bomberman.discovery.data.ServiceEvent.addEvent(builder, eventOffset);
  return xyz.bomberman.discovery.data.ServiceEvent.endServiceEvent(builder);
}

// Exports for Node.js and RequireJS
this.xyz = xyz;
