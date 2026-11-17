<?php

/**
 * Regras de texto e de tipo de publicação lidas do OrangeHRM 5.9,
 * commit 56e23b3, e isoladas para partição de equivalência, valor limite
 * e tabela de decisão.
 *
 * Origem:
 * - BuzzPostAPI::PARAM_RULE_TEXT_MAX_LENGTH = 65530
 * - BuzzPostAPI::getTextValidationRuleForTextPost()
 *   STRING_TYPE + REQUIRED + STR_LENGTH [null, 65530]
 * - BuzzPostAPI::setBuzzPhotos() recusa 0 fotos e mais de 5
 * - BuzzCommentAPI::getValidationRuleForCreate()
 *   STRING_TYPE + STR_LENGTH, sem REQUIRED
 * - NotBlankStringType: is_string e não casa com /^\s+$/
 *   A string vazia não casa com esse regex (o + exige um caractere).
 * - Required / IsEmptyTrait: vazio é só null ou ''.
 * - Tipos aceitos: text, photo, video (BuzzShare::POST_TYPE_*).
 *
 * comentarioComoNoCodigo descreve o que essas classes fazem.
 * comentarioExigidoPelaJornada descreve a regra da jornada: comentário
 * sem texto não deveria ser gravado. A distância entre as duas é o
 * defeito D1, confirmado na API.
 */
final class RegrasDeTexto
{
    public const TAMANHO_MAXIMO = 65530;
    public const FOTOS_MAXIMO = 5;

    public static function comprimento(string $texto): int
    {
        if (function_exists('mb_strlen')) {
            return mb_strlen($texto, 'UTF-8');
        }
        $quantidade = preg_match_all('/./u', $texto);
        return $quantidade === false ? 0 : $quantidade;
    }

    /**
     * @return array{aceito: bool, motivo: ?string}
     */
    public static function postagem(?string $texto): array
    {
        if (!is_string($texto) || $texto === '' || preg_match('/^\s+$/', $texto) === 1) {
            return ['aceito' => false, 'motivo' => 'text'];
        }
        if (self::comprimento($texto) > self::TAMANHO_MAXIMO) {
            return ['aceito' => false, 'motivo' => 'text'];
        }
        return ['aceito' => true, 'motivo' => null];
    }

    /**
     * @return array{aceito: bool, motivo: ?string}
     */
    public static function comentarioComoNoCodigo(?string $texto): array
    {
        if (!is_string($texto) || preg_match('/^\s+$/', $texto) === 1) {
            return ['aceito' => false, 'motivo' => 'text'];
        }
        if (self::comprimento($texto) > self::TAMANHO_MAXIMO) {
            return ['aceito' => false, 'motivo' => 'text'];
        }
        return ['aceito' => true, 'motivo' => null];
    }

    /**
     * @return array{aceito: bool, motivo: ?string}
     */
    public static function comentarioExigidoPelaJornada(?string $texto): array
    {
        return self::postagem($texto);
    }

    /**
     * Tabela de decisão da criação. O conteúdo do link (YouTube, Vimeo,
     * Dailymotion) é decidido por BuzzEmbeddedURL, não por esta tabela:
     * aqui o link entra só como presente ou ausente.
     */
    public static function decidirCriacao(
        string $tipo,
        ?string $texto,
        bool $linkPresente,
        int $fotos
    ): string {
        if (!in_array($tipo, ['text', 'photo', 'video'], true)) {
            return 'rejeita';
        }

        if ($tipo === 'text') {
            return self::postagem($texto)['aceito'] ? 'aceita' : 'rejeita';
        }

        if ($texto !== null && !self::postagem($texto)['aceito']) {
            return 'rejeita';
        }

        if ($tipo === 'video') {
            return $linkPresente ? 'aceita' : 'rejeita';
        }

        if ($fotos < 1 || $fotos > self::FOTOS_MAXIMO) {
            return 'rejeita';
        }
        return 'aceita';
    }
}
