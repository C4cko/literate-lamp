import assert from "node:assert/strict";
import { before, describe, test } from "node:test";
import {
  apagarShare,
  comentar,
  entrar,
  listarComentarios,
  localizarShare,
  marcar,
  publicarTexto,
} from "./apoio.js";

const LIMITE = 65530;

describe("API Buzz — jornada publicar, comentar e apagar", () => {
  let sessao;

  before(async () => {
    sessao = await entrar();
  });

  test("IT-01 o feed autenticado responde 200", async () => {
    const feed = await sessao.api("/api/v2/buzz/feed?limit=1");
    assert.equal(feed.status, 200);
    assert.ok(Array.isArray(feed.corpo.data));
  });

  test("IT-02 estados da jornada: publicar, comentar, apagar comentário, apagar post", async () => {
    const texto = marcar("AB-jornada");
    const comentario = `comentario ${texto}`;

    const criado = await publicarTexto(sessao, texto);
    assert.equal(criado.status, 200, JSON.stringify(criado.corpo));
    const postId = criado.corpo.data.post.id;

    const achado = await localizarShare(sessao, { postId, texto });
    assert.equal(achado.status, 200);
    assert.ok(achado.item, "o post deveria aparecer no feed");
    const shareId = achado.item.id;
    try {
      assert.equal(achado.item.stats.numOfComments, 0);

      const novo = await comentar(sessao, shareId, comentario);
      assert.equal(novo.status, 200, JSON.stringify(novo.corpo));
      const commentId = novo.corpo.data.comment.id;

      const detalhe = await sessao.api(
        `/api/v2/buzz/shares/${shareId}/comments/${commentId}?model=detailed`,
      );
      const detalheJson = JSON.stringify(detalhe.corpo);
      assert.equal(detalhe.status, 200, detalheJson.slice(0, 400));
      assert.ok(
        detalheJson.includes(comentario),
        `comentário ausente em ${detalheJson.slice(0, 500)}`,
      );

      const semComentario = await sessao.api(
        `/api/v2/buzz/shares/${shareId}/comments/${commentId}`,
        { method: "DELETE" },
      );
      assert.equal(semComentario.status, 200);
      const aposApagar = await listarComentarios(sessao, shareId);
      assert.equal(aposApagar.corpo.meta?.total ?? 0, 0);

      const apagado = await apagarShare(sessao, shareId);
      assert.equal(apagado.status, 200);
      const sumiu = await localizarShare(sessao, { postId, texto });
      assert.equal(sumiu.item, null);
    } finally {
      const restante = await localizarShare(sessao, { postId, texto });
      if (restante.item) await apagarShare(sessao, restante.item.id);
    }
  });

  test("IT-03 texto vazio e só espaços são rejeitados na postagem", async () => {
    for (const texto of ["", " ", " \t "]) {
      const resposta = await publicarTexto(sessao, texto);
      assert.equal(resposta.status, 422, JSON.stringify(resposta.corpo));
      assert.deepEqual(resposta.corpo.error.data.invalidParamKeys, ["text"]);
    }
  });

  test("IT-04 o texto '0' é uma postagem válida", async () => {
    const resposta = await publicarTexto(sessao, "0");
    assert.equal(resposta.status, 200, JSON.stringify(resposta.corpo));
    const share = await localizarShare(sessao, { postId: resposta.corpo.data.post.id, texto: "0" });
    assert.equal(share.item.text, "0");
    await apagarShare(sessao, share.item.id);
  });

  test("IT-05 limites 65530 e 65531 na postagem", async () => {
    const noLimite = "L" + "a".repeat(LIMITE - 1);
    const aceito = await publicarTexto(sessao, noLimite);
    assert.equal(aceito.status, 200, JSON.stringify(aceito.corpo)?.slice(0, 300));
    const share = await localizarShare(sessao, { postId: aceito.corpo.data.post.id });
    assert.equal(share.item.text.length, LIMITE);
    await apagarShare(sessao, share.item.id);

    const acima = "L" + "a".repeat(LIMITE);
    const recusado = await publicarTexto(sessao, acima);
    assert.equal(recusado.status, 422);
    assert.ok(recusado.corpo.error.data.invalidParamKeys.includes("text"));
  });

  test("IT-06 tabela de decisão do tipo de post na API", async () => {
    const audio = await sessao.api("/api/v2/buzz/posts", {
      method: "POST",
      json: { type: "audio", text: marcar("AB-audio") },
    });
    assert.equal(audio.status, 422);
    assert.ok(audio.corpo.error.data.invalidParamKeys.includes("type"));

    const fotoSemArquivo = await sessao.api("/api/v2/buzz/posts", {
      method: "POST",
      json: { type: "photo", text: marcar("AB-foto") },
    });
    assert.equal(fotoSemArquivo.status, 422);

    const video = await sessao.api("/api/v2/buzz/posts", {
      method: "POST",
      json: { type: "video", link: "https://youtu.be/jNQXAC9IVRw" },
    });
    assert.equal(video.status, 200, JSON.stringify(video.corpo)?.slice(0, 300));
    const share = await localizarShare(sessao, { postId: video.corpo.data.post.id });
    assert.ok(share.item);
    await apagarShare(sessao, share.item.id);
  });

  test("IT-07 validação de link de vídeo", async () => {
    const ok = await sessao.api(
      "/api/v2/buzz/validation/links?url=" +
        encodeURIComponent("https://www.youtube.com/watch?v=jNQXAC9IVRw"),
    );
    assert.equal(ok.status, 200);
    assert.equal(ok.corpo.data.valid, true);
    assert.equal(ok.corpo.data.embeddedURL, "https://www.youtube.com/embed/jNQXAC9IVRw");

    const semId = await sessao.api(
      "/api/v2/buzz/validation/links?url=" +
        encodeURIComponent("https://www.youtube.com/watch?v="),
    );
    assert.equal(semId.status, 200);
    assert.equal(semId.corpo.data.valid, false);

    const outro = await sessao.api(
      "/api/v2/buzz/validation/links?url=" + encodeURIComponent("https://example.com/video/1"),
    );
    assert.equal(outro.status, 200);
    assert.equal(outro.corpo.data.valid, false);
  });

  test("IT-08 comentário só com espaços é rejeitado; comentário curto é gravado", async () => {
    const texto = marcar("AB-comentario");
    const criado = await publicarTexto(sessao, texto);
    const share = await localizarShare(sessao, { postId: criado.corpo.data.post.id, texto });
    try {
      const espacos = await comentar(sessao, share.item.id, "   ");
      assert.equal(espacos.status, 422);
      assert.deepEqual(espacos.corpo.error.data.invalidParamKeys, ["text"]);

      const curto = await comentar(sessao, share.item.id, "👍");
      assert.equal(curto.status, 200);
    } finally {
      await apagarShare(sessao, share.item.id);
    }
  });
});
