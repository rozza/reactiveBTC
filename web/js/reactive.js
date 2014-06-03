function graphPlot() {

    var n = 140, svg, path, axis, yAxis, data = [], glyph, diffs=[0.001],
        margin = {top: 20, right: 20, bottom: 20, left: 80},
        width = 960 - margin.left - margin.right,
        height = 200 - margin.top - margin.bottom,
        duration = 750,
        now = new Date(Date.now() - duration),
        lastSeen = 0, timeout = false;

    var x = d3.time.scale()
        .domain([now - (n - 2) * duration, now - duration])
        .range([0, width]);

    var y = d3.scale.linear()
        .range([height, 0]);
    y.domain([-0.001, 0.001]);

    var line = d3.svg.line()
        .interpolate("basis-open")
        .x(function(d, i) { return x(now - (n - 1 - i) * duration); })
        .y(function(d, i) { return y(d); });


    var graph = function(nm) {
        glyph = d3.select(nm.split(" ")[0] + " .glyphicon");
        svg = d3.select(nm).append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
            .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        svg.append("defs").append("clipPath")
            .attr("id", "clip")
            .append("rect")
            .attr("width", width)
            .attr("height", height);

        axis = svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .call(x.axis = d3.svg.axis().scale(x).orient("bottom"));

        yAxis = svg.append("g")
            .attr("class", "y axis")
            .call(d3.svg.axis().scale(y).orient("left"));

        path = svg.append("g")
            .attr("clip-path", "url(#clip)")
            .append("path")
            .data([data])
            .attr("class", "line");

        return graph;
    };


    graph.tick = function (newData) {
        // push a new data point onto the back
        var animateClass = "glyphicon-refresh-animate";
        glyph.classed(animateClass, true);

        // Only rotate if not rotating
        if(!timeout){
            timeout = true;
            setTimeout( function() {
                glyph.classed(animateClass, false);
                timeout = false;
            }, 1001 );
        }

        if (data.length == 0){
            d3.range(n).map(function(i){data.push(0);});
            graph.redraw();
        }

        if (newData == lastSeen) return;
        lastSeen = newData;
        data.push(newData);
        data.shift();
        var yl = d3.extent(data);
        diffs.push((yl[1] - yl[0]) / 4);
        diffs.shift();
    };

    graph.redraw = function() {

        // update the domains
        now = new Date();
        x.domain([now - (n - 2) * duration, now - duration]);

        var yl = d3.extent(data),
            ydiff = (yl[1] - yl[0]) / 4;

        var last = data.pop();
        data.push(last);
        data.push(last);
        data.shift();

        var diff = d3.max(diffs),
            yMin = yl[0] - diff,
            yMax = yl[1] + diff;

        // Set the domain for data
        y.domain([yMin > 0 ? yMin : 0, yMax]);

        // redraw the line
        svg.select(".line")
            .attr("d", line)
            .attr("transform", null);

        // slide the x-axis left
        axis.transition()
            .duration(duration)
            .ease("linear")
            .call(x.axis);

        if (ydiff != 0) {
            yAxis
                .attr("class", "y axis")
                .attr("transform", null)
                .transition()
                .duration(duration)
                .ease("linear")
                .call(d3.svg.axis().scale(y).orient("left"));
        }

        // slide the line left
        path.transition()
            .duration(duration)
            .ease("linear")
            .attr("transform", "translate(" + x(now - (n - 2) * duration) + ")")
            .each("end", graph.redraw);
    };

    return graph;

}

var tries = 0;
function loadData() {
    // Let us open a web socket
    var ws = new WebSocket("ws://localhost:8080/trades/");
    ws.onmessage = function (evt) {
        var rawData = JSON.parse(evt.data);
        var ticker = graphs[rawData.channel];
        var newData = parseFloat(rawData.trade.topbuy.price);
        ticker.tick(newData);
    };
    ws.onclose = function() {
        console.log("CLOSED");
        tries++;
        if (tries < 5) {
            setTimeout(loadData, 2000);
        }
    };
}
