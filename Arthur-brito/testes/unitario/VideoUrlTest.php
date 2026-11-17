<?php

use OrangeHRM\Buzz\Dto\BuzzVideoURL\BuzzEmbeddedURL;
use PHPUnit\Framework\TestCase;

/**
 * Testes unitários das classes originais de URL do Buzz.
 * Cada linha da partição é um formato aceito ou rejeitado por
 * BuzzEmbeddedURL, EmbeddedURLForYoutube, EmbeddedURLForVimeo
 * e EmbeddedURLForDailymotion.
 */
class VideoUrlTest extends TestCase
{
    /**
     * @dataProvider urlsValidas
     */
    public function testUrlValidaProduzEmbed(string $url, string $embed): void
    {
        $parser = new BuzzEmbeddedURL($url);

        $this->assertTrue($parser->isValidURL(), $url);
        $this->assertSame($embed, $parser->getEmbeddedURL(), $url);
        $this->assertSame($embed, $parser->getEmbeddedURL(), 'a segunda leitura usa o valor já gerado');
    }

    public function urlsValidas(): array
    {
        return [
            'youtu.be com playlist' => [
                'https://youtu.be/JGkxRVEzUz0?list=PLW9NCQftUhFjMUjjWooQmvLGdkOgXr_pY',
                'https://www.youtube.com/embed/JGkxRVEzUz0',
            ],
            'youtube watch com tempo' => [
                'https://www.youtube.com/watch?v=4dDP_1lGbYs&t=4s',
                'https://www.youtube.com/embed/4dDP_1lGbYs',
            ],
            'youtube shorts' => [
                'https://www.youtube.com/shorts/Fsa34Mo439qI0',
                'https://www.youtube.com/embed/Fsa34Mo439qI0',
            ],
            'youtube embed já pronto' => [
                'https://www.youtube.com/embed/jNQXAC9IVRw',
                'https://www.youtube.com/embed/jNQXAC9IVRw',
            ],
            'youtube http legado' => [
                'http://www.youtube.com/v/-wtIMTCHWuI?version=3&autohide=1',
                'https://www.youtube.com/embed/-wtIMTCHWuI',
            ],
            'vimeo numerico' => [
                'https://vimeo.com/732689681',
                'https://player.vimeo.com/video/732689681',
            ],
            'vimeo já embedado' => [
                'https://player.vimeo.com/video/732689681',
                'https://player.vimeo.com/video/732689681',
            ],
            'dailymotion longo' => [
                'https://www.dailymotion.com/video/x8fi3pm?playlist=x6hzyp',
                'https://www.dailymotion.com/embed/video/x8fi3pm',
            ],
            'dailymotion curto' => [
                'https://dai.ly/x2rnn2s',
                'https://www.dailymotion.com/embed/video/x2rnn2s',
            ],
        ];
    }

    /**
     * @dataProvider urlsInvalidas
     */
    public function testUrlInvalida(string $url): void
    {
        $parser = new BuzzEmbeddedURL($url);

        $this->assertFalse($parser->isValidURL(), $url);
        $this->assertFalse($parser->isValidURL(), 'repetir a leitura não torna a URL válida');
    }

    public function testUrlDeOutroSiteDevolveNulo(): void
    {
        $parser = new BuzzEmbeddedURL('https://example.com/video/123');

        $this->assertFalse($parser->isValidURL());
        $this->assertNull($parser->getEmbeddedURL());
    }

    public function urlsInvalidas(): array
    {
        return [
            'vazia' => [''],
            'site qualquer' => ['https://example.com/video/123'],
            'youtube sem identificador' => ['https://www.youtube.com/watch?v='],
            'youtu.be com barra no lugar do id' => ['https://youtu.be/###/s3unDu3QfW0?t=13'],
            'vimeo sem número' => ['https://vimeo.com/###/253989945'],
            'dailymotion sem id' => ['https://dai.ly/###/253989945'],
            'texto que apenas cita o youtube' => ['https://example.com/?q=youtube'],
        ];
    }
}
