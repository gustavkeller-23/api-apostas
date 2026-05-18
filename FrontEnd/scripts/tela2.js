const API = 'https://lutadores-api-22f61a69f511.herokuapp.com';

// ── Dados ──
const artes = { '1': 'Boxe', '2': 'Karatê', '3': 'Muay Thai' };
let lutadores = [];
let editandoId = null;

// ── Criptografia RSA-OAEP (Web Crypto API) ──
// Par de chaves gerado uma vez por sessão de browser
let rsaPrivateKey = null;
let rsaPublicKey  = null;
let handshakeDone = false;

/**
 * Gera um par de chaves RSA-OAEP 2048 bits.
 * Usa SHA-256 para hash e MGF1 — igual à configuração do SecurityUtils.java.
 */
async function gerarParDeChaves() {
  const keyPair = await crypto.subtle.generateKey(
    {
      name: 'RSA-OAEP',
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: 'SHA-256',
    },
    true,        // exportável
    ['encrypt', 'decrypt']
  );
  rsaPrivateKey = keyPair.privateKey;
  rsaPublicKey  = keyPair.publicKey;
}

/**
 * Exporta a chave pública no formato SPKI (Base64) e envia ao servidor.
 * O servidor armazenará essa chave para criptografar todas as respostas seguintes.
 */
async function realizarHandshake() {
  if (handshakeDone) return;

  await gerarParDeChaves();

  // Exporta a chave pública como SPKI (SubjectPublicKeyInfo) em Base64
  const spki   = await crypto.subtle.exportKey('spki', rsaPublicKey);
  const base64  = btoa(String.fromCharCode(...new Uint8Array(spki)));

  const res = await fetch(`${API}/handshake`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ publicKey: base64 })
  });

  if (!res.ok) {
    throw new Error(`Handshake falhou: HTTP ${res.status}`);
  }

  handshakeDone = true;
  console.log('🔑 Handshake RSA realizado com sucesso.');
}

/**
 * Descriptografa a resposta do servidor.
 *
 * O servidor retorna um JSON array de chunks RSA criptografados:
 *   ["base64chunk1", "base64chunk2", ...]
 *
 * Cada chunk é descriptografado individualmente com a privateKey RSA
 * e os bytes são concatenados → UTF-8 → JSON.parse.
 */
async function decryptResponse(res) {
  const encrypted = res.headers.get('X-Content-Encrypted');

  // Fallback: se servidor não criptografou (handshake não feito ainda)
  if (encrypted === 'false' || encrypted === null) {
    return res.json();
  }

  const chunksJson = await res.text();

  // Parse do array de strings Base64
  let chunks;
  try {
    chunks = JSON.parse(chunksJson);
  } catch {
    throw new Error('Resposta criptografada inválida (não é JSON array).');
  }

  if (!Array.isArray(chunks)) {
    throw new Error('Formato inesperado: esperado JSON array de chunks.');
  }

  // Descriptografa cada chunk e concatena os bytes
  const allBytes = [];
  for (const chunk of chunks) {
    const cipherBytes = Uint8Array.from(atob(chunk), c => c.charCodeAt(0));
    const plainBuffer = await crypto.subtle.decrypt(
      { name: 'RSA-OAEP' },
      rsaPrivateKey,
      cipherBytes
    );
    allBytes.push(...new Uint8Array(plainBuffer));
  }

  const plainText = new TextDecoder().decode(new Uint8Array(allBytes));
  return JSON.parse(plainText);
}

// ── Inicialização ──
document.addEventListener('DOMContentLoaded', async () => {
  try {
    await realizarHandshake();
  } catch (err) {
    showToast('Erro no handshake de segurança!', 'erro');
    console.error(err);
  }

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

// ── Carregar lutadores da API ──
async function carregarLutadores() {
  try {
    const res = await fetch(`${API}/lutadores`);
    if (res.ok) {
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
  const id      = document.getElementById('inputId').value.trim();
  const nome    = document.getElementById('inputNome').value.trim();
  const cat     = document.getElementById('inputCategoria').value;
  const apelido = document.getElementById('inputApelido').value.trim();
  const arte    = document.getElementById('inputArte').value;

  if (!id || !nome || !cat || !apelido || !arte) {
    showToast('Preencha todos os campos!', 'erro');
    return;
  }

  try {
    if (editandoId) {
      // ── Modo edição: PUT /lutadores/:id?nome=X&apelido=X&categoria=X&arte=X ──
      const qs  = new URLSearchParams({ nome, apelido, categoria: cat, arte }).toString();
      const res = await fetch(`${API}/lutadores/${editandoId}?${qs}`, { method: 'PUT' });

      if (res.ok) {
        await carregarLutadores();
        cancelarEdicao();
        showToast('Lutador atualizado com sucesso!', 'ok');

        document.querySelectorAll('.tab, .tab-content').forEach(el => el.classList.remove('active'));
        document.querySelector('[data-tab="listar"]').classList.add('active');
        document.getElementById('tab-listar').classList.add('active');
        renderTabela();
      } else {
        const dados = await decryptResponse(res).catch(() => ({}));
        showToast(dados.erro || 'Erro ao atualizar lutador!', 'erro');
      }

    } else {
      // ── Modo cadastro: POST /lutadores?id=X&nome=X&apelido=X&categoria=X&arte=X ──
      const qs  = new URLSearchParams({ id, nome, apelido, categoria: cat, arte }).toString();
      const res = await fetch(`${API}/lutadores?${qs}`, { method: 'POST' });

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

  editandoId = l.id;

  document.querySelectorAll('.tab, .tab-content').forEach(el => el.classList.remove('active'));
  document.querySelector('[data-tab="cadastrar"]').classList.add('active');
  document.getElementById('tab-cadastrar').classList.add('active');

  document.getElementById('inputId').value        = l.id;
  document.getElementById('inputId').disabled     = true;
  document.getElementById('inputNome').value      = l.nome;
  document.getElementById('inputCategoria').value = l.categoria;
  document.getElementById('inputApelido').value   = l.apelido;
  document.getElementById('inputArte').value      = l.arte;

  document.getElementById('form-titulo').textContent       = 'EDITAR LUTADOR';
  document.getElementById('btn-principal').textContent     = 'SALVAR ALTERAÇÕES';
  document.getElementById('btn-principal').classList.add('editando');
  document.getElementById('btn-cancelar').style.display   = 'block';
}

function cancelarEdicao() {
  editandoId = null;
  document.getElementById('inputId').disabled = false;
  document.getElementById('form-titulo').textContent     = 'NOVO CADASTRO DE LUTADOR';
  document.getElementById('btn-principal').textContent   = 'CADASTRAR LUTADOR';
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