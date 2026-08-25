import firebase_admin
from firebase_admin import credentials
from firebase_admin import messaging

from .config import FIREBASE_CREDENTIALS_FILE

# Firebase initialization
if not firebase_admin._apps:
    cred = credentials.Certificate(
        FIREBASE_CREDENTIALS_FILE
    )

    firebase_admin.initialize_app(cred)


# Pantry sharing notification
def send_pantry_share_notification(
    fcm_token: str,
    request_id: int,
    requester_name: str,
) -> str:

    message = messaging.Message(
        notification=messaging.Notification( #notification for android to display
            title="New Pantry Sharing Request",
            body=f"{requester_name} wants to join your pantry",
        ),
        data={ #data to be used
            "type": "pantry_share_request",
            "request_id": str(request_id),
        },
        token = fcm_token,
    )

    response = messaging.send(message)
    return response

''' PAYLOAD
{
    "notification": {
        "title": "Pantry sharing request",
        "body": "Bob wants to join your pantry"
    },
    "data": {
        "type": "pantry_share_request",
        "request_id": "123"
    }
}
'''