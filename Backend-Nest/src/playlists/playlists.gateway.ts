import { forwardRef, Inject, Logger } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import {
  OnGatewayConnection,
  OnGatewayDisconnect,
  WebSocketGateway,
  WebSocketServer,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { PlaylistsService } from './playlists.service';

// Real-time multi-user playlist editing.
// Clients connect to ws://.../socket.io with query { playlistId, token: <jwt access token> }
// and get a message whenever someone adds/removes/reorders a track.
@WebSocketGateway({ cors: { origin: '*' } })
export class PlaylistsGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer() server: Server;
  private readonly logger = new Logger(PlaylistsGateway.name);

  constructor(
    private readonly jwt: JwtService,
    @Inject(forwardRef(() => PlaylistsService)) private readonly playlists: PlaylistsService,
  ) {}

  async handleConnection(client: Socket) {
    const { token, playlistId } = client.handshake.query as { token?: string; playlistId?: string };
    if (!token || !playlistId) return client.disconnect(true);

    try {
      const payload = this.jwt.verify(token);
      if (payload.type !== 'access') return client.disconnect(true);
      const canView = await this.playlists.canViewAsUserId(Number(playlistId), payload.sub);
      if (!canView) return client.disconnect(true);

      client.join(`playlist_${playlistId}`);
    } catch {
      client.disconnect(true);
    }
  }

  handleDisconnect() {
    // socket.io handles leaving rooms automatically on disconnect.
  }

  broadcast(playlistId: number, event: string, data: Record<string, unknown>) {
    this.server?.to(`playlist_${playlistId}`).emit('update', { event, ...data });
  }
}
