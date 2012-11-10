var map = L.map('map').setView([51.505, -0.09], 13);

L.tileLayer('http://server.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/tile/{z}/{y}/{x}', {
    attribution: 'Map data &copy; ESRI',
    maxZoom: 15
}).addTo(map);

map.setView([-18.40,46.62], 5);

var jsonData = {}

map.on('click', function(evt) {
    var url = "http://46.51.133.104:8080/svc/species?y=" + evt.latlng.lat + "&x=" + evt.latlng.lng;
    $.getJSON(url, function(j) {
        jsonData = j;
        render();
    });
});

function getSpecies() {
    return $("#species").val().toLowerCase();
}

function getYears() {
    var years = [];
    if ($("#y1950").is(":checked")) {
        years.push("1950");
    }
    if ($("#y2000").is(":checked")) {
        years.push("2000");
    }
    if ($("#y2080").is(":checked")) {
        years.push("2080");
    }
    if ($("#y2080a").is(":checked")) {
        years.push("2080a");
    }
    if ($("#y2080b").is(":checked")) {
        years.push("2080b");
    }
    return years;
}

function getClimates() {
    var climates = [];
    if ($("#forest").is(":checked")) {
        climates.push("forest");
    }
    if ($("#only").is(":checked")) {
        climates.push("only");
    }
    return climates;
}

function render() {
    var dd = jsonData;
    var tbl = "<table>";
    var years =  getYears();
    var clims = getClimates();
    var species = getSpecies();
    for(var spc in dd) {
        if (species == null || species.length == 0 ||
            spc.toLowerCase().indexOf(species.toLowerCase()) >= 0) {
            tbl += "<tr><td>" + spc + "</td></tr>";
            for(var year in dd[spc]) {
                if (years.indexOf(year) >= 0) {
                    tbl += "<tr><td>"
                    tbl += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                    tbl += year + "</td></tr>";

                    for(var ctype in dd[spc][year]) {
                        if (clims.indexOf(ctype) >= 0) {
                            var data = dd[spc][year][ctype][0];

                            if (data.value > 50) {
                                var b = 100 - 50*(100 - data.value) / 100;
                                var clr = "hsl(120," + data.value + "% ," + b +"%);";
                            } else { 
                                var v = 100 - data.value;
                                var b = 50*(data.value + 50) / 100 + 50;
                                var clr = "hsl(0," + v + "% ," + b + "%);";
                            }
                            var clazz = "background-color: " + clr;
                            //var clazz = data.valid ? "good" : "bad";
                            tbl += "<tr><td style=\"" + clazz + "\">"
                            tbl += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                            tbl += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"
                            tbl += ctype + "</td>"
                            tbl += "<td style=\"" + clazz + "\">" + data.valid + " (" + data.value + ")</td></tr>";
                        }
                    }
                }
            }
        }
    }
    tbl += tbl + "</table>";
    $("#tbl").html(tbl);
}

$("#y1950").click(render);
$("#y2000").click(render);
$("#y2080").click(render);
$("#y2080a").click(render);
$("#y2080b").click(render);
$("#forest").click(render);
$("#only").click(render);
$("#species").keydown(render);
$("#species").keyup(render);
