var token = window.location.href.split("#")[1].split("access_token=")[1].split("&")[0];
var xhr = new XMLHttpRequest();
xhr.open("GET", "https://discord.com/api/users/@me", true);
xhr.onreadystatechange = function(){
    if(this.readyState == 4 && this.status == 200) {
        userIDReceived(this.responseText);
    }
};
xhr.setRequestHeader("Authorization", "Bearer " + token);
xhr.send();

function userIDReceived(responseText) {
    var userData = JSON.parse(responseText);
    var userID = userData.id;

    var xhr2 = new XMLHttpRequest();
    xhr2.open("GET", "/lastMessage/" + token, true);
    xhr2.onreadystatechange = function(){
        if(this.readyState == 4 && this.status == 200) {
           document.getElementById('lastMessage').innerHTML = xhr2.responseText;
        }
    };
    xhr2.send();

    var xhr3 = new XMLHttpRequest();
    xhr3.open("GET", "/voiceChannel/" + token, true);
    xhr3.onreadystatechange = function(){
        if(this.readyState == 4 && this.status == 200) {
            document.getElementById('voiceChannel').innerHTML = xhr3.responseText;
            if(xhr3.responseText.includes("ID")) {
                document.getElementById('join').disabled = false;
                document.getElementById('join').addEventListener("click", function() {
                    joinCall();
                })
            }
        }
    };
    xhr3.send();

}

//Join with the TOKEN, not an ID, to ensure this is authorized
//The server will call the API to ensure the token is accurate
function joinCall() {
    var xhr2 = new XMLHttpRequest();
    xhr2.open("POST", "/join/" + token, true);
    xhr2.send();
}