$(document).ready(function () {

    $('#protein-table').DataTable({
        deferRender:    true,
        ajax: {
            url: 'api/proteins/datatable',
            // dataSrc: '',
            data: function (data) {
                return JSON.stringify(data);
            },
            processData: false,
            dataType: "json",
            contentType: "application/json;charset=UTF-8",
            type: "POST"
        },
        searchDelay: 400,
        "processing": true,
        "serverSide": true,
        columnDefs: [
            {
                targets: 0,
                className: 'dt-left mono-font-body',
                data: "accession",
                render: function ( data, type, row, meta ) {
                    return '<span class="align-middle">' +
                        '<div style="display: flex; flex-wrap: wrap; align-items: center; justify-content: left;">' +
                        '<a href="//www.uniprot.org/uniprot/' + data + '" target="_blank" class="mr-2 uniprot-url"></a>' +
                        data +
                        '</div>' +
                        '</span>';
                }
            },
            {
                targets: 1,
                data: "size",
                className: 'dt-right mono-font-body'
            },
            {
                targets: 2,
                data: "accession",
                searchable: false,
                orderable: false,
                className: 'dt-center',
                render: function ( data, type, row, meta ) {
                    return '<span class="align-middle">' +
                        '<a href="proteins/' + data + '" target="_blank" class="align-middle mr-2"><i class="fas fa-chart-area mr-1"></i>Matrix</a>' +
                        '<a href="api/proteins/' + data + '/download" target="_blank" class="align-middle"><i class="fas fa-file-download mr-1"></i>Download</a>' +
                        '</span>';
                }
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