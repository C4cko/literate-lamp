<?php

namespace OrangeHRM\Core\Traits\Service;

/**
 * Substitui o trait de produção, que exige o contêiner do Symfony.
 * Devolve o TextHelperService original, sem dependências.
 */
trait TextHelperTrait
{
    protected function getTextHelper(): \OrangeHRM\Core\Service\TextHelperService
    {
        return new \OrangeHRM\Core\Service\TextHelperService();
    }
}
