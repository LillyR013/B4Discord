var token = window.location.href.split("#")[1].split("access_token=")[1].split("&")[0];
getUserData();

function getUserData(){
    var xhr = new XMLHttpRequest();
    xhr.open("GET", "/lastMessage/" + token, true);
    xhr.onreadystatechange = function(){
        if(this.readyState == 4 && this.status == 200) {
           document.getElementById('lastMessage').innerHTML = xhr.responseText;
        }
    };
    xhr.send();

    var xhr2 = new XMLHttpRequest();
    xhr2.open("GET", "/voiceChannel/" + token, true);
    xhr2.onreadystatechange = function(){
        if(this.readyState == 4 && this.status == 200) {
            document.getElementById('voiceChannel').innerHTML = xhr2.responseText;
            if(xhr2.responseText.includes("ID")) {
                document.getElementById('join').disabled = false;
                document.getElementById('join').addEventListener("click", function() {
                    joinCall();
                })
            }
        }
    };
    xhr2.send();
}

//Join with the TOKEN, not an ID, to ensure this is authorized
//The server will call the API to ensure the token is accurate
function joinCall() {
    var xhr = new XMLHttpRequest();
    xhr.open("POST", "/join/" + token, true);
    xhr.send();
}