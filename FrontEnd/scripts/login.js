const API = 'http://localhost:8080';

// ── Criptografia RSA-OAEP (Web Crypto API) ──
let rsaPrivateKey = null;
let rsaPublicKey  = null;
let handshakeDone = false;

async function gerarParDeChaves() {
  const keyPair = await crypto.subtle.generateKey(
    {
      name: 'RSA-OAEP',
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: 'SHA-256',
    },
    true,
    ['encrypt', 'decrypt']
  );
  rsaPrivateKey = keyPair.privateKey;
  rsaPublicKey  = keyPair.publicKey;
}

async function realizarHandshake() {
  if (handshakeDone) return;
  await gerarParDeChaves();
  const spki   = await crypto.subtle.exportKey('spki', rsaPublicKey);
  const base64  = btoa(String.fromCharCode(...new Uint8Array(spki)));
  const res = await fetch(`${API}/handshake`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ publicKey: base64 })
  });
  if (!res.ok) throw new Error(`Handshake falhou: HTTP ${res.status}`);
  handshakeDone = true;
}

async function decryptResponse(res) {
  const encrypted = res.headers.get('X-Content-Encrypted');
  if (encrypted === 'false' || encrypted === null) {
    return res.json();
  }
  const chunksJson = await res.text();
  const chunks = JSON.parse(chunksJson);
  const allBytes = [];
  for (const chunk of chunks) {
    const cipherBytes = Uint8Array.from(atob(chunk), c => c.charCodeAt(0));
    const plainBuffer = await crypto.subtle.decrypt({ name: 'RSA-OAEP' }, rsaPrivateKey, cipherBytes);
    allBytes.push(...new Uint8Array(plainBuffer));
  }
  return JSON.parse(new TextDecoder().decode(new Uint8Array(allBytes)));
}

// ── Inicialização ──
document.addEventListener('DOMContentLoaded', async () => {
  try {
    await realizarHandshake();
    console.log('🔑 Handshake RSA realizado com sucesso.');
  } catch (err) {
    console.error('Handshake falhou:', err);
  }
});

// ── Toggle senha ──
function togglePwd(id) {
  const el = document.getElementById(id);
  el.type = el.type === 'password' ? 'text' : 'password';
}

// ── Alternar telas ──
function mostrarCadastro() {
  document.getElementById('tela-login').style.display = 'none';
  document.getElementById('tela-cadastro').style.display = 'block';
}

function mostrarLogin() {
  document.getElementById('tela-cadastro').style.display = 'none';
  document.getElementById('tela-login').style.display = 'block';
  limparCadastro();
}

// ── Login ──
async function fazerLogin() {
  const usuario = document.getElementById('loginUsuario').value.trim();
  const senha   = document.getElementById('pwdInput').value;

  if (!usuario || !senha) {
    showToast('Preencha usuário e senha!', 'erro');
    return;
  }

  try {
    const res = await fetch(`${API}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ usuario, senha })
    });

    if (res.ok) {
      showToast('Login realizado com sucesso!', 'ok');
      setTimeout(() => { window.location.href = 'pages/tela2.html'; }, 1000);
    } else {
      const dados = await decryptResponse(res).catch(() => ({}));
      showToast(dados.mensagem || dados.erro || 'Usuário ou senha incorretos!', 'erro');
    }
  } catch (err) {
    showToast('Erro ao conectar com o servidor!', 'erro');
    console.error(err);
  }
}

// ── Criar conta ──
async function criarConta() {
  const usuario = document.getElementById('cadUsuario').value.trim();
  const senha   = document.getElementById('cadSenha').value;
  const conf    = document.getElementById('cadSenhaConf').value;

  if (!usuario || !senha || !conf) {
    showToast('Preencha todos os campos!', 'erro');
    return;
  }
  if (senha !== conf) {
    showToast('As senhas não coincidem!', 'erro');
    return;
  }
  if (senha.length < 4) {
    showToast('Senha deve ter ao menos 4 caracteres!', 'erro');
    return;
  }

  try {
    const res = await fetch(`${API}/cadastro`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ usuario, senha })
    });

    if (res.ok) {
      showToast('Conta criada com sucesso!', 'ok');
      setTimeout(() => mostrarLogin(), 1500);
    } else {
      const dados = await decryptResponse(res).catch(() => ({}));
      showToast(dados.mensagem || dados.erro || 'Erro ao criar conta!', 'erro');
    }
  } catch (err) {
    showToast('Erro ao conectar com o servidor!', 'erro');
    console.error(err);
  }
}

function limparCadastro() {
  ['cadUsuario', 'cadSenha', 'cadSenhaConf'].forEach(id => {
    const el = document.getElementById(id);
    if (el) { el.value = ''; el.type = id === 'cadUsuario' ? 'text' : 'password'; }
  });
}

// ── Toast ──
function showToast(msg, tipo) {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = 'toast show ' + tipo;
  setTimeout(() => { t.className = 'toast'; }, 2800);
}