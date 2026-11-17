import assert from "node:assert/strict";
import { execFile } from "node:child_process";
import { test } from "node:test";
import { chromium } from "playwright";

const base = process.env.ORANGEHRM_BASE ?? "https://opensource-demo.orangehrmlive.com";
const usuario = process.env.ORANGEHRM_USER ?? "Admin";
const senha = process.env.ORANGEHRM_PASSWORD ?? "admin123";

async function abrirBuzz() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  page.setDefaultTimeout(20_000);
  await page.goto(`${base}/web/index.php/auth/login`, { waitUntil: "domcontentloaded" });
  await page.getByPlaceholder("Username").fill(usuario);
  await page.getByPlaceholder("Password").fill(senha);
  await page.getByRole("button", { name: "Login" }).click();
  await page.waitForURL("**/dashboard/**");
  await page.goto(`${base}/web/index.php/buzz/viewBuzz`, { waitUntil: "domcontentloaded" });
  await page.getByPlaceholder("What's on your mind?").first().waitFor();
  return { browser, page };
}

/** Cartão do feed. O texto fica em .orangehrm-buzz-post-body, fora de .orangehrm-buzz-post. */
function cartao(page, texto) {
  return page.locator(".orangehrm-buzz").filter({ hasText: texto });
}

async function fechar(browser) {
  await Promise.race([
    browser.close().catch(() => {}),
    new Promise((resolver) => setTimeout(resolver, 1500)),
  ]);
  await new Promise((resolver) => {
    execFile("taskkill", ["/IM", "headless_shell.exe", "/F"], () => resolver());
  });
}

test("ST-01 publica um texto, comenta e apaga a publicação", async () => {
  const { browser, page } = await abrirBuzz();
  const texto = `AB-st-${Date.now()}`;
  const comentario = `nota ${texto}`;
  try {
    await page.getByPlaceholder("What's on your mind?").first().fill(texto);
    const pedido = page.waitForResponse(
      (res) => res.url().includes("/api/v2/buzz/posts") && res.request().method() === "POST",
      { timeout: 20_000 },
    );
    await page.getByRole("button", { name: "Post", exact: true }).click();
    const resposta = await pedido;
    assert.equal(resposta.status(), 200, await resposta.text());

    const textoNaTela = page.getByText(texto, { exact: true }).first();
    const apareceuNaHora = await textoNaTela
      .waitFor({ timeout: 12_000 })
      .then(() => true)
      .catch(() => false);
    if (!apareceuNaHora) {
      await page.goto(`${base}/web/index.php/buzz/viewBuzz`, { waitUntil: "domcontentloaded" });
      await page.getByPlaceholder("What's on your mind?").first().waitFor();
      await textoNaTela.waitFor({ timeout: 20_000 });
    }
    const card = textoNaTela.locator(
      "xpath=ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' orangehrm-buzz ')][1]",
    );

    await card.locator(".bi-chat-text-fill").click();
    const campo = card.getByPlaceholder("Write your comment...");
    await campo.fill(comentario);
    await campo.press("Enter");
    await card.getByText(comentario, { exact: false }).waitFor();

    await card.locator(".orangehrm-buzz-post-header-config").first().locator(".bi-three-dots").click();
    await page.locator(".oxd-dropdown-menu").getByText("Delete Post", { exact: true }).click();
    await page.getByRole("button", { name: "Yes, Delete" }).click();
    await page.waitForFunction(
      (alvo) =>
        [...document.querySelectorAll("p.orangehrm-buzz-post-body-text")].every(
          (el) => el.textContent.trim() !== alvo,
        ),
      texto,
      { timeout: 20_000 },
    );
    assert.equal(await cartao(page, texto).count(), 0);
  } finally {
    await fechar(browser);
  }
});

test("ST-02 a postagem vazia não entra no feed", async () => {
  const { browser, page } = await abrirBuzz();
  try {
    const posts = page.locator(".orangehrm-buzz");
    await posts.first().waitFor();
    const quantidade = await posts.count();
    await page.getByRole("button", { name: "Post", exact: true }).click();
    await page.waitForTimeout(1500);
    assert.equal(await posts.count(), quantidade);
  } finally {
    await fechar(browser);
  }
});

test("ST-03 a postagem só com espaços não entra no feed", async () => {
  const { browser, page } = await abrirBuzz();
  try {
    const posts = page.locator(".orangehrm-buzz");
    await posts.first().waitFor();
    const quantidade = await posts.count();
    await page.getByPlaceholder("What's on your mind?").first().fill("   ");
    await page.getByRole("button", { name: "Post", exact: true }).click();
    await page.waitForTimeout(1500);
    assert.equal(await posts.count(), quantidade);
  } finally {
    await fechar(browser);
  }
});

test("ST-04 um link que não é vídeo não vira publicação", async () => {
  const { browser, page } = await abrirBuzz();
  const legenda = `AB-video-${Date.now()}`;
  try {
    await page.getByRole("button", { name: "Share Video" }).click();
    await page.getByPlaceholder("Paste Video URL").fill("https://example.com/nao-e-video");
    await page.getByPlaceholder("What's on your mind?").last().fill(legenda);
    await page.getByRole("button", { name: "Post", exact: true }).last().click();
    await page.waitForTimeout(2000);
    assert.equal(await cartao(page, legenda).count(), 0);
  } finally {
    await fechar(browser);
  }
});
