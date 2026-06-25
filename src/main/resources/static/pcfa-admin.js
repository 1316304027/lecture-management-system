/** コース割当：検索付きスクロール一覧 */
function pcfaFilterUserPicker(pickerId, q) {
    q = (q || '').toLowerCase();
    document.querySelectorAll('#' + pickerId + 'List .pcfa-user-list-item').forEach(el => {
        const s = (el.dataset.search || '').toLowerCase();
        el.style.display = !q || s.includes(q) ? '' : 'none';
    });
}

function pcfaSelectUserPicker(pickerId, btn) {
    document.querySelectorAll('#' + pickerId + 'List .pcfa-user-list-item').forEach(el => {
        el.classList.remove('selected');
    });
    btn.classList.add('selected');
    document.getElementById(pickerId + 'UserId').value = btn.dataset.id;
}

function pcfaFilterAssignedTable(input, tableId) {
    const q = (input.value || '').toLowerCase();
    document.querySelectorAll('#' + tableId + ' tbody tr[data-search]').forEach(tr => {
        tr.style.display = (tr.dataset.search || '').toLowerCase().includes(q) ? '' : 'none';
    });
}
