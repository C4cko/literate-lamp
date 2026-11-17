// Reproduz as gravações do Chrome Recorder (pasta gravacoes/) com o @puppeteer/replay.
// O texto dos posts usa {{ID}}, trocado aqui por um número único a cada execução,
// assim o teste só mexe no post que ele mesmo criou.
//
// Uso: npm test            (todas as gravações)
//      node executar.mjs ts01-jornada-completa.json
// Variáveis: CHROME=caminho do Chrome, HEADLESS=false para ver o navegador,
//            LENTO=150 para deixar as ações mais devagar.

import fs from 'node:fs';
import path from 'node:path';
import puppeteer from 'puppeteer';
import { createRunner, PuppeteerRunnerExtension } from '@puppeteer/replay';

// Tira uma captura de tela sempre antes de abrir o menu do post. Fica a última,
// que é o estado logo antes da exclusão (no TS-03, já depois do "No, Cancel").
class ComEvidencia extends PuppeteerRunnerExtension {
  constructor(browser, page, opcoes, arquivoImagem) {
    super(browser, page, opcoes);
    this.arquivoImagem = arquivoImagem;
  }

  async beforeEachStep(step, flow) {
    if (step.type === 'click' && JSON.stringify(step.selectors).includes('bi-three-dots')) {
      await new Promise((r) => setTimeout(r, 500));
      await this.page.screenshot({ path: this.arquivoImagem });
    }
    await super.beforeEachStep?.(step, flow);
  }
}

const PASTA = path.join(import.meta.dirname, 'gravacoes');
const RESULTADOS = path.join(import.meta.dirname, '..', 'resultados');
const CHROME = process.env.CHROME || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const BASE = 'https://opensource-demo.orangehrmlive.com/web/index.php';

const arquivos = process.argv.slice(2).length
  ? process.argv.slice(2)
  : fs.readdirSync(PASTA).filter((f) => f.endsWith('.json')).sort();

fs.mkdirSync(RESULTADOS, { recursive: true });

const browser = await puppeteer.launch({
  executablePath: fs.existsSync(CHROME) ? CHROME : undefined,
  headless: process.env.HEADLESS !== 'false',
  slowMo: Number(process.env.LENTO || 0), // pausa em ms entre as ações, para acompanhar na tela
  defaultViewport: null,
});

let falhas = 0;
for (const arquivo of arquivos) {
  const id = Date.now();
  const texto = fs.readFileSync(path.join(PASTA, arquivo), 'utf8').replaceAll('{{ID}}', String(id));
  const gravacao = JSON.parse(texto);
  // cada gravação em um contexto novo (sem cookies da anterior), pois todas começam pelo login
  const contexto = await browser.createBrowserContext();
  const page = await contexto.newPage();
  const inicio = Date.now();
  try {
    const runner = await createRunner(gravacao, new ComEvidencia(browser, page, { timeout: 30000 }, path.join(RESULTADOS, arquivo.replace('.json', '.png'))));
    await runner.run();
    console.log(`PASSOU  ${gravacao.title}  (${((Date.now() - inicio) / 1000).toFixed(1)} s)`);
  } catch (erro) {
    falhas++;
    console.log(`FALHOU  ${gravacao.title}\n        ${erro.message.split('\n')[0]}`);
    await page.screenshot({ path: path.join(RESULTADOS, arquivo.replace('.json', '-falha.png')) }).catch(() => {});
  } finally {
    await limpar(page, id);
    await contexto.close();
  }
}

await browser.close();
console.log(`\n${arquivos.length - falhas} de ${arquivos.length} gravações passaram`);
process.exit(falhas ? 1 : 0);

// Se o teste parar no meio, o post dele fica no feed. Aqui ele é apagado pela API,
// usando a sessão do próprio navegador e só se o texto tiver o ID desta execução.
async function limpar(page, id) {
  await page.evaluate(async (base, id) => {
    const r = await fetch(`${base}/api/v2/buzz/feed?limit=50&offset=0&sortOrder=DESC&sortField=share.createdAtUtc`);
    if (!r.ok) return;
    for (const s of (await r.json()).data) {
      if ((s.text || '').startsWith('JM-TS') && s.text.endsWith(`-${id}`)) {
        await fetch(`${base}/api/v2/buzz/shares/${s.id}`, { method: 'DELETE' });
      }
    }
  }, BASE, id).catch(() => {});
}
