const fs = require("fs");
const path = require("path");
const fse = require("fs-extra");

const mm = require("music-metadata");

const inputFolder = "./music";

const outputFolder = "./output";

const outputMusic = "./output/music";


fse.ensureDirSync(outputFolder);
fse.ensureDirSync(outputMusic);


let songs = [];


async function generate(){


const files = fs.readdirSync(inputFolder);



for(let i=0;i<files.length;i++){


const file = files[i];


if(
file.endsWith(".mp3") ||
file.endsWith(".wav")
){


const filePath =
path.join(inputFolder,file);



const metadata =
await mm.parseFile(filePath);



const common =
metadata.common;


const format =
metadata.format;



// copy music

fse.copySync(
filePath,
path.join(outputMusic,file)
);



songs.push({


id:i+1,


title:
common.title ||
path.parse(file).name,


artist:
common.artist ||
"Unknown",


album:
common.album ||
"Unknown",


genre:
common.genre ||
"Unknown",


year:
common.year ||
"",


track:
common.track?.no || "",


duration:
Math.floor(format.duration || 0),


durationText:
format.duration ?
new Date(format.duration*1000)
.toISOString()
.substring(14,19)
:
"00:00",


bitrate:
format.bitrate || "",


sampleRate:
format.sampleRate || "",


file:
`music/${file}`


});



}

}



fs.writeFileSync(

"./output/songs.json",

JSON.stringify(
songs,
null,
4
)

);


console.log(
"Music folder created with metadata"
);


}


generate();