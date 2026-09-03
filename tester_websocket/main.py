import asyncio
import json
import websockets

## tester for websoket endpoint

async def listen():
    token = ""
    #token = ""
    url = f"ws://localhost:8000/ws/pantry?token={token}"

    async with websockets.connect(url) as ws:
        print("Connected!")

        while True:
            message = await ws.recv()
            data = json.loads(message)

            print("Received:", data)

asyncio.run(listen())
