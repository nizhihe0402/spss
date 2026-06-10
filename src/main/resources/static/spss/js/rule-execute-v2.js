/**
 * SPSS Rule Engine V2 执行规则页面脚本。
 *
 * 目标：
 * 1. 上传 CSV
 * 2. 调用 /api/v2/rules/execute
 * 3. 结果分两部分展示：通过学生 / 未通过学生
 * 4. Layui 上传不使用 obj.getFile()
 */
(function () {
    var selectedCsvFile = null;
    var selectedMappingFile = null;
    var selectedStudentFile = null;

    layui.use(['upload', 'table', 'layer'], function () {
        var upload = layui.upload;
        var layer = layui.layer;

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
            url: '/api/v2/rules/execute',
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
                renderSplitResult(res.data || {});
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

    function clearResult() {
        $('#resultSummary').html('');
        $('#passedPanel').hide();
        $('#failedPanel').hide();
        $('#legacyFieldResult').hide();
    }

    window.renderSplitResult = function (data) {
        var passedList = data.passedList || [];
        var failedList = data.failedList || [];

        var totalRows = data.totalRows || 0;
        var studentCount = data.studentCount || (passedList.length + failedList.length);
        var passedCount = data.passedCount || passedList.length;
        var failedCount = data.failedCount || failedList.length;

        $('#resultSummary').html(
            '<div class="summary-line">' +
            '总行数：<b>' + escapeHtml(totalRows) + '</b>，' +
            '学生数：<b>' + escapeHtml(studentCount) + '</b>，' +
            '通过：<b class="ok">' + escapeHtml(passedCount) + '</b>，' +
            '未通过：<b class="bad">' + escapeHtml(failedCount) + '</b>' +
            '</div>'
        );

        $('#passedPanel').show();
        $('#failedPanel').show();

        layui.table.render({
            elem: '#passedTable',
            data: passedList,
            page: true,
            limit: 20,
            limits: [20, 50, 100, 200],
            text: {none: '暂无通过学生'},
            cols: [[
                {field: 'studentId', title: '学生ID', width: 180, templet: function(d){return escapeHtml(d.studentId || d.studentKey || '');}},
                {field: 'studentName', title: '姓名', minWidth: 140, templet: function(d){return escapeHtml(d.studentName || '');}}
            ]]
        });

        var failedRows = [];
        for (var i = 0; i < failedList.length; i++) {
            var item = failedList[i] || {};
            var violations = item.violations || [];
            if (violations.length === 0) {
                failedRows.push({
                    studentId: item.studentId,
                    studentName: item.studentName,
                    studentKey: item.studentKey,
                    ruleNames: '',
                    messages: '',
                    detail: ''
                });
                continue;
            }

            for (var j = 0; j < violations.length; j++) {
                var v = violations[j] || {};
                failedRows.push({
                    studentId: item.studentId,
                    studentName: item.studentName,
                    studentKey: item.studentKey,
                    level: v.level || '',
                    ruleCode: v.ruleCode || '',
                    ruleName: v.ruleName || '',
                    message: v.message || '',
                    lineNo: v.lineNo || '',
                    fieldName: v.fieldName || '',
                    tableId: v.tableId || '',
                    questionId: v.questionId || '',
                    optionId: v.optionId || '',
                    content: v.content || ''
                });
            }
        }

        layui.table.render({
            elem: '#failedTable',
            data: failedRows,
            page: true,
            limit: 20,
            limits: [20, 50, 100, 200],
            text: {none: '暂无未通过学生'},
            cols: [[
                {field: 'studentId', title: '学生ID', width: 170, templet: function(d){return escapeHtml(d.studentId || d.studentKey || '');}},
                {field: 'studentName', title: '姓名', width: 120, templet: function(d){return escapeHtml(d.studentName || '');}},
                {field: 'level', title: '级别', width: 85},
                {field: 'ruleName', title: '违反规则', width: 180},
                {field: 'message', title: '原因说明', minWidth: 420},
                {field: 'lineNo', title: '行', width: 80},
                {field: 'fieldName', title: '字段', width: 120},
                {field: 'questionId', title: '问题ID', width: 120},
                {field: 'optionId', title: '选项ID', width: 120}
            ]]
        });
    };

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
