def send_pantry_share_notification(
    fcm_token: str,
    request_id: int,
    requester_name: str,
):
    # TODO
    return "TODO"

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