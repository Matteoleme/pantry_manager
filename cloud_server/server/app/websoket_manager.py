from fastapi import WebSocket, WebSocketDisconnect
from dataclasses import dataclass
from .auth import validate_access_token_on_websocket

@dataclass
class UserConnection:
    access_token: str
    session_version: int

class PantryConnectionManager:
    def __init__(self):
        self.connections: dict[
            str,
            dict[WebSocket, UserConnection]
        ] = {}

    # CONNECT
    async def connect(
            self, 
            token_share: str, 
            websocket: WebSocket,
            access_token: str,
            session_version: int,
    ):
        await websocket.accept()

        if token_share not in self.connections:
            self.connections[token_share] = {}

        self.connections[token_share][websocket] = UserConnection(
            access_token=access_token,
            session_version=session_version,
        )

    # DISCONNECT
    def disconnect(
            self, 
            token_share: str, 
            websocket: WebSocket
    ):
        connections = self.connections.get(token_share)

        if not self.connections:
            return

        connections.pop(websocket, None)

        if not connections:
            del self.connections[token_share]

    # BROADCAST EVENT
    async def broadcast(
            self, 
            token_share: str, 
            message: dict
    ):
        connections = self.connections.get(token_share, {}).copy()
        for websocket, userConnection in connections.items():
            #### validate access_token before sending message to such user
            if not await validate_access_token_on_websocket(userConnection.access_token, userConnection.session_version):
                self.disconnect(token_share, websocket)
                try:
                    await websocket.close(code=1008)
                except Exception:
                    pass
                #go to next registered websocket for the token_share
                continue
                
            try:
                #access_token validated, send message
                await websocket.send_json(message)
            except Exception:
                self.disconnect(token_share, websocket)


pantry_manager = PantryConnectionManager()

'''
@app.websocket("/ws/pantry")
async def pantry_websocket(websocket: WebSocket):
    token = websocket.query_params.get("token")

    # Decode/validate JWT here
    user = get_user_from_token(token)

    if user is None:
        await websocket.close(code=1008)
        return

    await pantry_manager.connect(user.token_share, websocket)

    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        pantry_manager.disconnect(user.token_share, websocket)
'''