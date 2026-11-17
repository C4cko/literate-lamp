import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { apagarShare, comentar, entrar, localizarShare, marcar, publicarTexto } from "./apoio.js";

/**
 * Estes testes usam o oráculo da jornada, não o comportamento atual.
 * D1 falha porque a API grava comentário vazio (criação e edição).
 * D3 falha porque vídeo sem link responde 500, não 422.
 * D2 falha porque o parâmetro documentado "link" responde 501; o servidor lê "url".
 */
describe("defeitos observados na API do Buzz", () => {
  test("D1 criar comentário com texto vazio deve ser recusado", async () => {
    const sessao = await entrar();
    const texto = marcar("AB-d1");
    const criado = await publicarTexto(sessao, texto);
    const share = await localizarShare(sessao, { postId: criado.corpo.data.post.id, texto });
    try {
      const resposta = await comentar(sessao, share.item.id, "");
      assert.equal(
        resposta.status,
        422,
        `a API aceitou comentário vazio: HTTP ${resposta.status} ${JSON.stringify(resposta.corpo).slice(0, 180)}`,
      );
    } finally {
      await apagarShare(sessao, share.item.id);
    }
  });

  test("D1 editar comentário para texto vazio deve ser recusado", async () => {
    const sessao = await entrar();
    const texto = marcar("AB-d1-edicao");
    const criado = await publicarTexto(sessao, texto);
    const share = await localizarShare(sessao, { postId: criado.corpo.data.post.id, texto });
    try {
      const comentario = await comentar(sessao, share.item.id, "texto inicial");
      const commentId = comentario.corpo.data.comment.id;
      const edicao = await sessao.api(
        `/api/v2/buzz/shares/${share.item.id}/comments/${commentId}`,
        { method: "PUT", json: { text: "" } },
      );
      assert.equal(edicao.status, 422, JSON.stringify(edicao.corpo).slice(0, 180));
    } finally {
      await apagarShare(sessao, share.item.id);
    }
  });

  test("D3 vídeo sem link deve ser recusado com 422, não com erro interno", async () => {
    const sessao = await entrar();
    const resposta = await sessao.api("/api/v2/buzz/posts", {
      method: "POST",
      json: { type: "video", text: marcar("AB-d3") },
    });
    assert.equal(
      resposta.status,
      422,
      `HTTP ${resposta.status} ${JSON.stringify(resposta.corpo).slice(0, 300)}`,
    );
  });

  test("D2 o parâmetro documentado link deveria validar o vídeo", async () => {
    const sessao = await entrar();
    const resposta = await sessao.api(
      "/api/v2/buzz/validation/links?link=" +
        encodeURIComponent("https://youtu.be/jNQXAC9IVRw"),
    );
    assert.equal(resposta.status, 200, JSON.stringify(resposta.corpo).slice(0, 240));
    assert.equal(resposta.corpo.data?.valid, true);
  });
});
