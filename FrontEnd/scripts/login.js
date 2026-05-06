const API = 'http://10.69.131.54:8080';

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
  const senha = document.getElementById('pwdInput').value;

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
      const dados = await res.json().catch(() => ({}));
      showToast(dados.mensagem || 'Usuário ou senha incorretos!', 'erro');
    }
  } catch (err) {
    showToast('Erro ao conectar com o servidor!', 'erro');
    console.error(err);
  }
}

// ── Criar conta ──
async function criarConta() {
  const usuario = document.getElementById('cadUsuario').value.trim();
  const senha = document.getElementById('cadSenha').value;
  const conf = document.getElementById('cadSenhaConf').value;

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
      const dados = await res.json().catch(() => ({}));
      showToast(dados.mensagem || 'Erro ao criar conta!', 'erro');
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