const API = 'http://10.69.131.54:8080';

// ── Dados ──
const artes = { '1': 'Boxe', '2': 'Karatê', '3': 'Muay Thai' };
let lutadores = [];
let editandoId = null;

// ── Inicialização ──
document.addEventListener('DOMContentLoaded', async () => {
  await carregarLutadores();

  // Tabs
  document.querySelectorAll('.tab').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab, .tab-content').forEach(el => el.classList.remove('active'));
      btn.classList.add('active');
      document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
      if (btn.dataset.tab === 'listar') renderTabela();
      if (btn.dataset.tab === 'remover') renderSelectRemover();
    });
  });

  // Select remover: preview ao mudar
  document.getElementById('selectRemover').addEventListener('change', function () {
    const l = lutadores.find(x => String(x.id) === this.value);
    const box = document.getElementById('preview-remover');
    if (!l) { box.style.display = 'none'; return; }
    box.style.display = 'block';
    box.innerHTML = `
      <div class="preview-row"><span>ID:</span><strong>${l.id}</strong></div>
      <div class="preview-row"><span>Nome:</span><strong>${l.nome}</strong></div>
      <div class="preview-row"><span>Categoria:</span><strong>${l.categoria}</strong></div>
      <div class="preview-row"><span>Apelido:</span><strong>@${l.apelido}</strong></div>
      <div class="preview-row"><span>Arte:</span><strong>${artes[l.arte]}</strong></div>`;
  });
});

// ── Criptografia: AES-256-GCM (Web Crypto API) ──
// Deve ser idêntica à CHAVE_MESTRA do SecurityUtils.java
const CHAVE_HEX = '3132333435363738393031323334353637383930313233343536373839303132';

async function getChave() {
  const keyBytes = hexToBytes(CHAVE_HEX);
  return crypto.subtle.importKey('raw', keyBytes, { name: 'AES-GCM' }, false, ['decrypt']);
}

function hexToBytes(hex) {
  const arr = new Uint8Array(hex.length / 2);
  for (let i = 0; i < arr.length; i++)
    arr[i] = parseInt(hex.substr(i * 2, 2), 16);
  return arr;
}

// Descriptografa a resposta do servidor (Base64 → IV[12] + ciphertext → JSON)
async function decryptResponse(res) {
  const b64 = await res.text();
  const raw = Uint8Array.from(atob(b64), c => c.charCodeAt(0));
  const iv = raw.slice(0, 12);
  const ciphertext = raw.slice(12);
  const chave = await getChave();
  const plain = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, chave, ciphertext);
  return JSON.parse(new TextDecoder().decode(plain));
}

// ── Carregar lutadores da API ──
async function carregarLutadores() {
  try {
    const res = await fetch(`${API}/lutadores`);
    if (res.ok) {
      // Resposta é criptografada — descriptografar antes de usar
      lutadores = await decryptResponse(res);
      renderTabela();
    } else {
      showToast('Erro ao carregar lutadores!', 'erro');
    }
  } catch (err) {
    showToast('Erro ao conectar com o servidor!', 'erro');
    console.error(err);
  }
}

// ── Cadastrar / Salvar edição ──
async function cadastrarLutador() {
  const id = document.getElementById('inputId').value.trim();
  const nome = document.getElementById('inputNome').value.trim();
  const cat = document.getElementById('inputCategoria').value;
  const apelido = document.getElementById('inputApelido').value.trim();
  const arte = document.getElementById('inputArte').value;

  if (!id || !nome || !cat || !apelido || !arte) {
    showToast('Preencha todos os campos!', 'erro');
    return;
  }

  try {
    if (editandoId) {
      // ── Modo edição: PUT /lutadores/:id?nome=X&apelido=X&categoria=X&arte=X ──
      // Backend lê query params na URL, não o body JSON
      const qs = new URLSearchParams({ nome, apelido, categoria: cat, arte }).toString();
      const res = await fetch(`${API}/lutadores/${editandoId}?${qs}`, {
        method: 'PUT'
      });

      if (res.ok) {
        await carregarLutadores();
        cancelarEdicao();
        showToast('Lutador atualizado com sucesso!', 'ok');

        // Volta para aba listar
        document.querySelectorAll('.tab, .tab-content').forEach(el => el.classList.remove('active'));
        document.querySelector('[data-tab="listar"]').classList.add('active');
        document.getElementById('tab-listar').classList.add('active');
        renderTabela();
      } else {
        const dados = await decryptResponse(res).catch(() => ({}));
        showToast(dados.erro || 'Erro ao atualizar lutador!', 'erro');
      }

    } else {
      // ── Modo cadastro: POST /lutadores?nome=X&apelido=X&categoria=X&arte=X ──
      // Backend lê query params na URL, não o body JSON
      const qs = new URLSearchParams({ id, nome, apelido, categoria: cat, arte }).toString();
      const res = await fetch(`${API}/lutadores?${qs}`, {
        method: 'POST'
      });

      if (res.ok) {
        await carregarLutadores();
        limparForm();
        showToast('Lutador cadastrado com sucesso!', 'ok');
      } else {
        const dados = await decryptResponse(res).catch(() => ({}));
        showToast(dados.erro || 'Erro ao cadastrar lutador!', 'erro');
      }
    }
  } catch (err) {
    showToast('Erro ao conectar com o servidor!', 'erro');
    console.error(err);
  }
}

function limparForm() {
  ['inputId', 'inputNome', 'inputApelido'].forEach(i => document.getElementById(i).value = '');
  document.getElementById('inputCategoria').value = '';
  document.getElementById('inputArte').value = '';
}

// ── Editar ──
function editarLutador(id) {
  const l = lutadores.find(x => String(x.id) === String(id));
  if (!l) return;

  editandoId = l.id; // usa o id numérico do objeto

  document.querySelectorAll('.tab, .tab-content').forEach(el => el.classList.remove('active'));
  document.querySelector('[data-tab="cadastrar"]').classList.add('active');
  document.getElementById('tab-cadastrar').classList.add('active');

  document.getElementById('inputId').value = l.id;
  document.getElementById('inputId').disabled = true;
  document.getElementById('inputNome').value = l.nome;
  document.getElementById('inputCategoria').value = l.categoria;
  document.getElementById('inputApelido').value = l.apelido;
  document.getElementById('inputArte').value = l.arte;

  document.getElementById('form-titulo').textContent = 'EDITAR LUTADOR';
  document.getElementById('btn-principal').textContent = 'SALVAR ALTERAÇÕES';
  document.getElementById('btn-principal').classList.add('editando');
  document.getElementById('btn-cancelar').style.display = 'block';
}

function cancelarEdicao() {
  editandoId = null;
  document.getElementById('inputId').disabled = false;
  document.getElementById('form-titulo').textContent = 'NOVO CADASTRO DE LUTADOR';
  document.getElementById('btn-principal').textContent = 'CADASTRAR LUTADOR';
  document.getElementById('btn-principal').classList.remove('editando');
  document.getElementById('btn-cancelar').style.display = 'none';
  limparForm();
}

// ── Listar ──
function renderTabela() {
  const tbody = document.getElementById('tabela-lutadores');
  document.getElementById('badge-total').textContent = lutadores.length;

  if (!lutadores.length) {
    tbody.innerHTML = '<tr class="empty-row"><td colspan="6">Nenhum lutador cadastrado ainda.</td></tr>';
    return;
  }

  tbody.innerHTML = lutadores.map(l => `
    <tr>
      <td><span class="tag">${l.id}</span></td>
      <td>${l.nome}</td>
      <td><span class="cat-badge">Cat ${l.categoria}</span></td>
      <td class="apelido">@${l.apelido}</td>
      <td>${artes[l.arte] || l.arte}</td>
      <td class="acoes-col">
        <button class="icon-btn edit sm" title="Editar" onclick="editarLutador('${l.id}')">
          <svg viewBox="0 0 24 24"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
        </button>
        <button class="icon-btn danger sm" title="Remover" onclick="removerPorId('${l.id}')">
          <svg viewBox="0 0 24 24"><path d="M6 19a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
        </button>
      </td>
    </tr>`).join('');
}

// ── Remover ──
function renderSelectRemover() {
  const sel = document.getElementById('selectRemover');
  sel.innerHTML = '<option value="">- Selecione -</option>' +
    lutadores.map(l => `<option value="${l.id}">${l.id} – ${l.nome}</option>`).join('');
  document.getElementById('preview-remover').style.display = 'none';
}

async function removerLutador() {
  const id = document.getElementById('selectRemover').value;
  if (!id) { showToast('Selecione um lutador!', 'erro'); return; }
  await removerPorId(id);
  renderSelectRemover();
}

async function removerPorId(id) {
  try {
    const res = await fetch(`${API}/lutadores/${id}`, { method: 'DELETE' });

    if (res.ok) {
      await carregarLutadores();
      renderTabela();
      showToast('Lutador removido!', 'ok');
    } else {
      const dados = await decryptResponse(res).catch(() => ({}));
      showToast(dados.erro || 'Erro ao remover lutador!', 'erro');
    }
  } catch (err) {
    showToast('Erro ao conectar com o servidor!', 'erro');
    console.error(err);
  }
}

// ── Toast ──
function showToast(msg, tipo) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'toast show ' + tipo;
  setTimeout(() => { t.className = 'toast'; }, 2800);
}