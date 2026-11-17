<?php

use PHPUnit\Framework\TestCase;

/**
 * Partição de equivalência, valor limite e tabela de decisão das regras
 * de texto do Buzz. O oráculo está em RegrasDeTexto, extraído do código
 * do commit 56e23b3.
 */
class RegrasDeTextoTest extends TestCase
{
    /**
     * @dataProvider particoesDePostagem
     */
    public function testParticoesELimitesDaPostagem(?string $texto, bool $aceito, string $rotulo): void
    {
        $resultado = RegrasDeTexto::postagem($texto);
        $this->assertSame($aceito, $resultado['aceito'], $rotulo);
        if (!$aceito) {
            $this->assertSame('text', $resultado['motivo'], $rotulo);
        }
    }

    public function particoesDePostagem(): array
    {
        $limite = str_repeat('a', RegrasDeTexto::TAMANHO_MAXIMO);
        $acima = str_repeat('a', RegrasDeTexto::TAMANHO_MAXIMO + 1);
        $acentoNoLimite = str_repeat('á', RegrasDeTexto::TAMANHO_MAXIMO);

        return [
            'nulo, classe inválida' => [null, false, 'nulo'],
            'vazio, logo abaixo do mínimo' => ['', false, 'vazio'],
            'um caractere, limite inferior válido' => ['a', true, 'um'],
            'zero como texto, não é vazio' => ['0', true, 'zero'],
            'só espaços' => ['   ', false, 'espacos'],
            'espaço e tabulação' => [" \t ", false, 'whitespace'],
            'texto com espaços nas pontas' => ['  reunião  ', true, 'miolo'],
            'emoji' => ['👍', true, 'emoji'],
            'limite de 65530 caracteres' => [$limite, true, '65530'],
            'um acima do limite' => [$acima, false, '65531'],
            '65530 caracteres acentuados contam como caracteres' => [$acentoNoLimite, true, 'mb'],
        ];
    }

    public function testComentarioVazioPassaNaRegraDoCodigoEFalhaNaRegraDaJornada(): void
    {
        $codigo = RegrasDeTexto::comentarioComoNoCodigo('');
        $jornada = RegrasDeTexto::comentarioExigidoPelaJornada('');

        $this->assertTrue(
            $codigo['aceito'],
            'NotBlankStringType não rejeita a string vazia e o comentário não usa REQUIRED.'
        );
        $this->assertFalse(
            $jornada['aceito'],
            'A jornada exige texto no comentário.'
        );
    }

    public function testComentarioSoComEspacosEhRejeitadoPelasDuasRegras(): void
    {
        $this->assertFalse(RegrasDeTexto::comentarioComoNoCodigo(' ')['aceito']);
        $this->assertFalse(RegrasDeTexto::comentarioExigidoPelaJornada(' ')['aceito']);
    }

    public function testComentarioAusenteEhRejeitado(): void
    {
        $this->assertFalse(RegrasDeTexto::comentarioComoNoCodigo(null)['aceito']);
    }

    /**
     * @dataProvider tabelaDeCriacao
     */
    public function testTabelaDeDecisaoDaCriacao(
        string $tipo,
        ?string $texto,
        bool $link,
        int $fotos,
        string $esperado,
        string $rotulo
    ): void {
        $this->assertSame(
            $esperado,
            RegrasDeTexto::decidirCriacao($tipo, $texto, $link, $fotos),
            $rotulo
        );
    }

    public function tabelaDeCriacao(): array
    {
        $noLimite = str_repeat('n', RegrasDeTexto::TAMANHO_MAXIMO);
        $acima = str_repeat('n', RegrasDeTexto::TAMANHO_MAXIMO + 1);

        return [
            'texto válido' => ['text', 'bom dia', false, 0, 'aceita', 't1'],
            'texto vazio' => ['text', '', false, 0, 'rejeita', 't2'],
            'texto nulo' => ['text', null, false, 0, 'rejeita', 't3'],
            'texto só com espaços' => ['text', '  ', false, 0, 'rejeita', 't4'],
            'texto no limite' => ['text', $noLimite, false, 0, 'aceita', 't5'],
            'texto acima do limite' => ['text', $acima, false, 0, 'rejeita', 't6'],
            'texto zero' => ['text', '0', false, 0, 'aceita', 't7'],
            'vídeo com link e sem legenda' => ['video', null, true, 0, 'aceita', 't8'],
            'vídeo sem link' => ['video', null, false, 0, 'rejeita', 't9'],
            'vídeo com legenda acima do limite' => ['video', $acima, true, 0, 'rejeita', 't10'],
            'foto com uma imagem' => ['photo', null, false, 1, 'aceita', 't11'],
            'foto no limite de cinco' => ['photo', 'legenda', false, 5, 'aceita', 't12'],
            'foto sem imagem' => ['photo', null, false, 0, 'rejeita', 't13'],
            'foto acima de cinco' => ['photo', null, false, 6, 'rejeita', 't14'],
            'tipo fora do domínio' => ['audio', 'bom dia', false, 0, 'rejeita', 't15'],
        ];
    }
}
