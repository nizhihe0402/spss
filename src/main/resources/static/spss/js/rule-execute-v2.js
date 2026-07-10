(function () {
    var selectedCsvFile = null;
    var selectedMappingFile = null;
    var selectedStudentFile = null;

    layui.use(['upload', 'table', 'layer'], function () {
        var upload = layui.upload;
        var layer = layui.layer;

        loadScripts();

        upload.render({
            elem: '#csvFileBtn',
            auto: false,
            accept: 'file',
            exts: 'csv',
            choose: function (obj) {
                obj.preview(function (index, file) {
                    selectedCsvFile = file;
                    $('#csvFileName').text(file.name || '已选择CSV');
                });
            }
        });

        upload.render({
            elem: '#mappingFileBtn',
            auto: false,
            accept: 'file',
            exts: 'json|sql|txt',
            choose: function (obj) {
                obj.preview(function (index, file) {
                    selectedMappingFile = file;
                    $('#mappingFileName').text(file.name || '已选择映射文件');
                });
            }
        });

        upload.render({
            elem: '#studentFileBtn',
            auto: false,
            accept: 'file',
            exts: 'json|csv|txt',
            choose: function (obj) {
                obj.preview(function (index, file) {
                    selectedStudentFile = file;
                    $('#studentFileName').text(file.name || '已选择学生信息文件');
                });
            }
        });

        $('#executeBtn').on('click', function () {
            executeRule(layer);
        });
    });

    function executeRule(layer) {
        var scriptId = $('#scriptSelect').val();
        var tableId = $('#tableId').val();
        var projectId = $('#projectId').val();
        var year = $('#year').val();
        var fieldCheck = $('#fieldCheck').is(':checked') ? '1' : '0';

        if (!selectedCsvFile) {
            layer.msg('请先选择CSV数据文件');
            return;
        }

        var formData = new FormData();
        formData.append('csvFile', selectedCsvFile);
        formData.append('file', selectedCsvFile);
        formData.append('scriptId', scriptId || '');
        formData.append('tableId', tableId || '');
        formData.append('projectId', projectId || '');
        formData.append('year', year || '');
        formData.append('fieldCheck', fieldCheck);

        if (selectedMappingFile) {
            formData.append('mappingFile', selectedMappingFile);
        }
        if (selectedStudentFile) {
            formData.append('studentFile', selectedStudentFile);
        }

        $('#executeMsg').removeClass('error').text('执行中...');
        clearResult();

        $.ajax({
            url: '/api/execute/upload',
            type: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: function (res) {
                if (!res || (res.code !== 0 && res.code !== 200)) {
                    var msg = res && res.msg ? res.msg : '执行失败';
                    $('#executeMsg').addClass('error').text(msg);
                    layer.msg(msg);
                    return;
                }

                $('#executeMsg').removeClass('error').text('执行完成');
                renderRuleResult(res.data || {});
            },
            error: function (xhr) {
                var msg = '执行规则请求失败';
                if (xhr && xhr.responseJSON && xhr.responseJSON.msg) {
                    msg = xhr.responseJSON.msg;
                }
                $('#executeMsg').addClass('error').text(msg);
                layer.msg(msg);
            }
        });
    }

    function loadScripts() {
        $.getJSON('/api/scripts', function (scripts) {
            var select = $('#scriptSelect');
            select.empty().append('<option value="">请选择脚本</option>');
            (scripts || []).forEach(function (script) {
                select.append(
                    '<option value="' + escapeHtml(script.id) + '">#' +
                    escapeHtml(script.id) + ' ' + escapeHtml(script.name || '') +
                    '</option>'
                );
            });
        });
    }

    function clearResult() {
        $('#resultSummary').html('');
        $('#passedPanel').hide();
        $('#failedPanel').hide();
        $('#legacyFieldResult').hide();
    }

    window.renderRuleResult = function (data) {
        var rows = data.students || [];
        var studentCount = data.studentCount || rows.length;
        var ruleCount = data.ruleCount || 0;
        var passedStudentCount = data.passedStudentCount || 0;
        var failedStudentCount = data.failedStudentCount || 0;

        $('#resultSummary').html(
            '<div class="summary-line">' +
            '学生数：<b>' + escapeHtml(studentCount) + '</b>' +
            '规则数：<b>' + escapeHtml(ruleCount) + '</b>' +
            '全部通过学生：<b class="ok">' + escapeHtml(passedStudentCount) + '</b>' +
            '存在未通过学生：<b class="bad">' + escapeHtml(failedStudentCount) + '</b>' +
            '</div>'
        );

        $('#passedPanel').show();
        $('#failedPanel').hide();

        layui.table.render({
            elem: '#passedTable',
            data: rows,
            page: true,
            limit: 20,
            limits: [20, 50, 100, 200],
            text: {none: '暂无执行结果'},
            cols: [[
                {field: 'studentId', title: '学生ID', width: 170, templet: function (d) {
                    return escapeHtml(d.studentId || d.studentKey || '');
                }},
                {field: 'studentName', title: '姓名', width: 120, templet: function (d) {
                    return escapeHtml(d.studentName || '');
                }},
                {field: 'passedText', title: '通过', minWidth: 220, templet: multilineCell('passedText')},
                {field: 'failedText', title: '未通过', minWidth: 420, templet: multilineCell('failedText')}
            ]]
        });
    };

    window.renderSplitResult = window.renderRuleResult;

    function multilineCell(field) {
        return function (d) {
            return '<div style="white-space: pre-line; line-height: 22px;">' + escapeHtml(d[field] || '') + '</div>';
        };
    }

    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return '';
        }
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
})();
