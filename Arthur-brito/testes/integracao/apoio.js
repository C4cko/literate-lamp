/**
 * Operações da jornada em cima da API v2.
 * O feed devolve o id do share; a criação do post devolve só o id do post.
 */
import { SessaoOrangeHrm } from "./cliente.js";

export async function publicarTexto(sessao, texto) {
  const criado = await sessao.api("/api/v2/buzz/posts", {
    method: "POST",
    json: { type: "text", text: texto },
  });
  return criado;
}

export async function localizarShare(sessao, { postId, texto }) {
  const feed = await sessao.api("/api/v2/buzz/feed?limit=50");
  const item = feed.corpo?.data?.find(
    (post) => post.post?.id === postId || (texto !== undefined && post.text === texto),
  );
  return { status: feed.status, item: item ?? null };
}

export async function apagarShare(sessao, shareId) {
  if (!shareId) return null;
  return sessao.api(`/api/v2/buzz/shares/${shareId}`, { method: "DELETE" });
}

export async function comentar(sessao, shareId, texto) {
  return sessao.api(`/api/v2/buzz/shares/${shareId}/comments`, {
    method: "POST",
    json: { text: texto },
  });
}

export async function listarComentarios(sessao, shareId) {
  return sessao.api(`/api/v2/buzz/shares/${shareId}/comments`);
}

export function marcar(prefixo) {
  return `${prefixo}-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
}

export async function entrar() {
  const sessao = new SessaoOrangeHrm();
  await sessao.entrar();
  return sessao;
}
