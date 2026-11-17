<?php

/**
 * Carrega as classes originais do Buzz (commit 56e23b3).
 * O TextHelperTrait de produção é trocado pelo stub antes de
 * AbstractBuzzVideoURL ser carregado. Os parsers não são reescritos.
 */

$sut = __DIR__ . '/sut';

if (!is_file($sut . '/BuzzEmbeddedURL.php')) {
    fwrite(STDERR, "Classes do SUT ausentes. Execute: node testes/unitario/baixar-sut.mjs\n");
    exit(1);
}

require __DIR__ . '/TextHelperTraitStub.php';
require $sut . '/TextHelperService.php';
require $sut . '/InvalidURLException.php';
require $sut . '/AbstractBuzzVideoURL.php';
require $sut . '/EmbeddedURLForYoutube.php';
require $sut . '/EmbeddedURLForVimeo.php';
require $sut . '/EmbeddedURLForDailymotion.php';
require $sut . '/BuzzEmbeddedURL.php';
require __DIR__ . '/RegrasDeTexto.php';
