<?php

/**
 * T2 VERVAL (PUCRS): UT-05 e UT-06 de docs/casos-de-teste.md.
 * Diferente do EmployeeAPITest original, o oráculo aqui confere os EFEITOS da criação no banco
 * (contador de IDs e foto gravada), não só o JSON devolvido.
 */

namespace OrangeHRM\Tests\Pim\T2AddEmployee;

use OrangeHRM\Core\Api\V2\Exception\BadRequestException;
use OrangeHRM\Core\Service\IDGeneratorService;
use OrangeHRM\Entity\EmpPicture;
use OrangeHRM\Entity\Employee;

/**
 * @group Pim
 * @group T2AddEmployee
 */
class EmployeeCreateBehaviourTest extends AddEmployeeApiTestCase
{
    protected function setUp(): void
    {
        parent::setUp();
        $this->loadFixtures();
    }

    private function nextSuggestedId(): string
    {
        // Na aplicação cada requisição tem seu EntityManager. No teste, a entidade UniqueId lida antes ficaria
        // em cache (identity map) e o UPDATE em DQL feito por incrementId() não apareceria; por isso o clear().
        $this->getEntityManager()->clear();
        // mesma chamada que SaveEmployeeController usa para pré-preencher o formulário
        return (new IDGeneratorService())->getNextID(Employee::class, false);
    }

    /**
     * UT-05 (controle): sem foto, o fluxo passa por EmployeeService::saveNewEmployee(), que incrementa o contador.
     */
    public function testUT05CriarSemFotoAvancaOIdSugerido(): void
    {
        $this->assertSame('0008', $this->nextSuggestedId(), 'pré-condição da fixture');

        $body = ['firstName' => 'Ana', 'lastName' => 'Silva', 'employeeId' => '0008'];
        $this->createApi($body)->create();

        $this->assertSame('0009', $this->nextSuggestedId());
    }

    /**
     * UT-05: com foto, o próximo ID sugerido também deve avançar.
     * FALHA no commit 56e23b3b: defeito D1 (docs/defeitos.md). O ramo com foto de EmployeeAPI::create()
     * salva via EmployeePictureService e nunca chama IDGeneratorService::incrementId().
     *
     * @group defeito-conhecido
     */
    public function testUT05CriarComFotoAvancaOIdSugerido(): void
    {
        $this->assertSame('0008', $this->nextSuggestedId(), 'pré-condição da fixture');

        $body = [
            'firstName' => 'Ana',
            'lastName' => 'Silva',
            'employeeId' => '0008',
            'empPicture' => self::pngAttachment(),
        ];
        $this->createApi($body)->create();

        $this->assertSame(
            '0009',
            $this->nextSuggestedId(),
            'Defeito D1: após cadastrar com foto, o sistema volta a sugerir o Employee Id já usado'
        );
    }

    /**
     * UT-05 (complemento): a foto é de fato gravada e ligada ao novo funcionário.
     */
    public function testUT05FotoEhGravadaParaONovoFuncionario(): void
    {
        $body = [
            'firstName' => 'Ana',
            'lastName' => 'Silva',
            'empPicture' => self::pngAttachment('perfil.png'),
        ];
        $result = $this->createApi($body)->create()->normalize();

        $this->getEntityManager()->clear();
        $picture = $this->repository(EmpPicture::class)->find($result['empNumber']);
        $this->assertInstanceOf(EmpPicture::class, $picture);
        $this->assertSame('perfil.png', $picture->getFilename());
        $this->assertSame('image/png', $picture->getFileType());
    }

    /**
     * UT-06: usuário ESS não pode criar funcionário (E9) e nada é gravado.
     */
    public function testUT06UsuarioSemPermissaoNaoCriaFuncionario(): void
    {
        $antes = count($this->repository(Employee::class)->findAll());
        $api = $this->createApi(['firstName' => 'Ana', 'lastName' => 'Silva'], self::ESS_USER_ID);

        try {
            $api->create();
            $this->fail('Esperava BadRequestException');
        } catch (BadRequestException $e) {
            $this->assertSame('Logged in User Not Allowed to Create an Employee', $e->getMessage());
        }
        $this->assertCount($antes, $this->repository(Employee::class)->findAll());
    }
}
