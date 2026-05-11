const API = 'http://localhost:8080';

// ── Chave pública do SERVIDOR (buscada ao carregar a página) ──
// Usada para criptografar login/senha antes de enviar
let servidorPublicKey = null;

// ── Chave privada do BROWSER (para descriptografar respostas) ──
let rsaPrivateKey = null;
let rsaPublicKey  = null;
let handshakeDone = false;

// ─────────────────────────────────────────────────────────────
// 1. Busca a chave pública RSA do servidor
// ─────────────────────────────────────────────────────────────
async function buscarChaveServidor() {
  const res = await fetch(`${API}/chave-publica`);
  if (!res.ok) throw new Error('Não foi possível buscar a chave pública do servidor.');

  const dados = await res.json();
  const keyBytes = Uint8Array.from(atob(dados.publicKey), c => c.charCodeAt(0));

  servidorPublicKey = await crypto.subtle.importKey(
    'spki',
    keyBytes,
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    false,
    ['encrypt']   // browser só criptografa com ela, nunca descriptografa
  );
  console.log('🔐 Chave pública do servidor importada.');
}

// ─────────────────────────────────────────────────────────────
// 2. Gera par de chaves do BROWSER e faz handshake com servidor
//    (para que o servidor criptografe as respostas para nós)
// ─────────────────────────────────────────────────────────────
async function realizarHandshake() {
  if (handshakeDone) return;

  const kp = await crypto.subtle.generateKey(
    { name: 'RSA-OAEP', modulusLength: 2048, publicExponent: new Uint8Array([1,0,1]), hash: 'SHA-256' },
    true, ['encrypt', 'decrypt']
  );
  rsaPrivateKey = kp.privateKey;
  rsaPublicKey  = kp.publicKey;

  const spki   = await crypto.subtle.exportKey('spki', rsaPublicKey);
  const base64  = btoa(String.fromCharCode(...new Uint8Array(spki)));

  const res = await fetch(`${API}/handshake`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ publicKey: base64 })
  });
  if (!res.ok) throw new Error(`Handshake falhou: HTTP ${res.status}`);

  handshakeDone = true;
  console.log('🔑 Handshake concluído: respostas do servidor serão criptografadas.');
}

// ─────────────────────────────────────────────────────────────
// 3. Criptografa um texto com a chave pública do SERVIDOR
//    (divide em chunks de 190 bytes — limite do RSA-2048 OAEP)
// ─────────────────────────────────────────────────────────────
async function encryptForServer(texto) {
  if (!servidorPublicKey) throw new Error('Chave pública do servidor não carregada.');

  const bytes = new TextEncoder().encode(texto);
  const CHUNK = 190;
  const chunks = [];

  for (let i = 0; i < bytes.length; i += CHUNK) {
    const slice = bytes.slice(i, i + CHUNK);
    const encrypted = await crypto.subtle.encrypt({ name: 'RSA-OAEP' }, servidorPublicKey, slice);
    chunks.push(btoa(String.fromCharCode(...new Uint8Array(encrypted))));
  }
  return chunks; // array de strings Base64
}

// ─────────────────────────────────────────────────────────────
// 4. Descriptografa uma resposta criptografada do servidor
// ─────────────────────────────────────────────────────────────
async function decryptResponse(res) {
  const encrypted = res.headers.get('X-Content-Encrypted');
  if (encrypted === 'false' || encrypted === null) return res.json();

  const chunks = JSON.parse(await res.text());
  const allBytes = [];
  for (const chunk of chunks) {
    const cipherBytes = Uint8Array.from(atob(chunk), c => c.charCodeAt(0));
    const plain = await crypto.subtle.decrypt({ name: 'RSA-OAEP' }, rsaPrivateKey, cipherBytes);
    allBytes.push(...new Uint8Array(plain));
  }
  return JSON.parse(new TextDecoder().decode(new Uint8Array(allBytes)));
}

// ─────────────────────────────────────────────────────────────
// Inicialização: busca chave do servidor + faz handshake
// ─────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  try {
    await buscarChaveServidor();
    await realizarHandshake();
  } catch (err) {
    console.error('Erro na inicialização de segurança:', err);
    showToast('Erro ao inicializar segurança!', 'erro');
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

// ── Login (credenciais criptografadas com chave pública do servidor) ──
async function fazerLogin() {
  const usuario = document.getElementById('loginUsuario').value.trim();
  const senha   = document.getElementById('pwdInput').value;

  if (!usuario || !senha) {
    showToast('Preencha usuário e senha!', 'erro');
    return;
  }

  try {
    // Criptografa as credenciais com a chave pública do servidor
    const credenciais = JSON.stringify({ usuario, senha });
    const chunks = await encryptForServer(credenciais);

    const res = await fetch(`${API}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(chunks)   // envia array de chunks criptografados
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

// ── Criar conta (credenciais criptografadas com chave pública do servidor) ──
async function criarConta() {
  const usuario = document.getElementById('cadUsuario').value.trim();
  const senha   = document.getElementById('cadSenha').value;
  const conf    = document.getElementById('cadSenhaConf').value;

  if (!usuario || !senha || !conf) { showToast('Preencha todos os campos!', 'erro'); return; }
  if (senha !== conf)               { showToast('As senhas não coincidem!', 'erro'); return; }
  if (senha.length < 4)             { showToast('Senha deve ter ao menos 4 caracteres!', 'erro'); return; }

  try {
    const credenciais = JSON.stringify({ usuario, senha });
    const chunks = await encryptForServer(credenciais);

    const res = await fetch(`${API}/cadastro`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(chunks)
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