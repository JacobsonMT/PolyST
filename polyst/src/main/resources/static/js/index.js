$(document).ready(function () {

    $('#protein-table').DataTable({
        columnDefs: [
            {
                targets: 0,
                className: 'dt-left'
            },
            {
                targets: 1,
                className: 'dt-right'
            },
            {
                targets: 2,
                className: 'dt-center'
            }
        ]
    });

    $("#protein-form").submit(function(event) {
        event.preventDefault();
        let proteinRequests = [];
        let lines = $("textarea.input").val().split('\n');
        let trimmed = [];
        for(let i = 0;i < lines.length;i++){
            let l = $.trim(lines[i]).split(":");
            if (l.length > 1) {
                proteinRequests.push({"accession": l[0], "location": parseInt(l[1]), "ref": l[2], "alt": l[3]});
                trimmed.push(l);
            }
        }

        $.ajax( {
            cache : false,
            type : 'POST',
            url : '/api/proteins',
            data : JSON.stringify(proteinRequests),
            contentType : "application/json"
        } ).then(function (data) {
            let output = [];
            trimmed.forEach(function(l, k) {
                output.push(l.join(":") + "\t" + data[k]);
            });
            $("textarea.output").val( output.join("\n") );
        });

    });

});