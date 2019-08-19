$(document).ready(function () {

    $('#protein-table').DataTable({
        deferRender:    true,
        ajax: {
            url: '/api/taxa/' + taxa.id + '/proteins/datatable',
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
                className: 'text-left mono-font-body',
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
                className: 'text-right mono-font-body'
            },
            {
                targets: 2,
                data: "accession",
                searchable: false,
                orderable: false,
                className: 'text-center',
                render: function ( data, type, row, meta ) {
                    return '<span class="align-middle">' +
                        '<a href="/taxa/' + taxa.id + '/proteins/' + data + '" target="_blank" class="align-middle mr-2"><i class="fas fa-chart-area mr-1"></i>Matrix</a>' +
                        '<a href="/api/taxa/' + taxa.id + '/proteins/' + data + '/download" target="_blank" class="align-middle"><i class="fas fa-file-download mr-1"></i>Download</a>' +
                        '</span>';
                }
            }
        ]
    });

    var dtable = $('#protein-table').dataTable().api();
    var searchWait = 0;
    var searchWaitInterval;
    $('.dataTables_filter input')
        .unbind() // Unbind previous default bindings
        .bind("input", function(e) { // Bind our desired behavior
            var item = $(this);
            searchWait = 0;

            if($(item).val() == "") {
                dtable.search("").draw();
                return;
            }

            if(!searchWaitInterval) searchWaitInterval = setInterval(function(){
                if(searchWait>=3){
                    clearInterval(searchWaitInterval);
                    searchWaitInterval = '';
                    searchTerm = $(item).val();
                    dtable.search(searchTerm).draw();
                    searchWait = 0;
                }
                searchWait++;
            },200);

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
            url : '/api/taxa/' + taxa.id + '/proteins',
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