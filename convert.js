const fs = require("fs");
const path = require("path");


const musicFolder = "./music";


let songs = [];


fs.readdirSync(musicFolder)
.forEach((file,index)=>{

    if(
        file.endsWith(".mp3") ||
        file.endsWith(".wav")
    ){

        songs.push({

            id:index+1,

            title:path.parse(file).name,

            artist:"Unknown Artist",

            file:`music/${file}`,

            image:"images/default.jpg"

        });

    }

});


fs.writeFileSync(
    "songs.json",
    JSON.stringify(songs,null,4)
);


console.log("songs.json created");