let ws = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    }
    else {
        $("#conversation").hide();
    }
    $("#greetings").html("");
}

function connect() {
    ws = new WebSocket("ws://localhost:4567/ws/game");
    ws.onopen = evt => {
                    setConnected(true);
                    console.log('Connected');
                };


    ws.onmessage = evt => {
                    const event = JSON.parse(e.data);
                      console.log(event);
                      //showGreeting(JSON.parse(greeting.body).content);
                };
}

function disconnect() {
    ws.close();
}

function sendName() {
    ws.send(JSON.stringify({
      requestType: "login",
      payload: {ssoTicket: $("#name").val()}
    }));
}

function showGreeting(message) {
    $("#greetings").append("<tr><td>" + message + "</td></tr>");
}

$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $( "#connect" ).click(() => connect());
    $( "#disconnect" ).click(() => disconnect());
    $( "#send" ).click(() => sendName());
});