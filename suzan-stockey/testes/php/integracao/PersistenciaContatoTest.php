<?php
namespace Suzan\EmergencyContacts;

use OrangeHRM\Entity\Employee;
use OrangeHRM\Entity\EmpEmergencyContact;
use OrangeHRM\ORM\Doctrine;
use OrangeHRM\Pim\Service\EmpEmergencyContactService;
use PHPUnit\Framework\TestCase;

class PersistenciaContatoTest extends TestCase
{
    private $em;
    private int $empNumber;
    private EmpEmergencyContactService $service;

    protected function setUp(): void
    {
        self::assertTrue(defined('ENVIRONMENT') && ENVIRONMENT === 'test');
        $this->em = Doctrine::getEntityManager();
        $this->em->clear();
        $this->em->getConnection()->beginTransaction();
        // Deixar o banco atribuir o identificador; rollback nao reinicia AUTO_INCREMENT.
        $employee = new Employee();
        $employee->setEmployeeId('T2-Suzan-' . uniqid());
        $employee->setFirstName('Teste');
        $employee->setLastName('T2 Suzan');
        $this->em->persist($employee);
        $this->em->flush();
        $this->empNumber = $employee->getEmpNumber();
        $this->service = new EmpEmergencyContactService();
    }

    protected function tearDown(): void
    {
        if ($this->em) {
            $connection = $this->em->getConnection();
            if ($connection->isTransactionActive()) {
                $connection->rollBack();
            }
            $this->em->clear();
        }
        parent::tearDown();
    }

    private function criarContato(): int
    {
        $contact = new EmpEmergencyContact();
        $contact->setEmployee($this->em->getReference(Employee::class, $this->empNumber));
        $contact->setName('Contato Ficticio T2');
        $contact->setRelationship('Irma');
        $contact->setHomePhone('51999990000');
        $contact->setMobilePhone('');
        $contact->setOfficePhone('');
        $saved = $this->service->saveEmpEmergencyContact($contact);
        return (int) $saved->getSeqNo();
    }

    public function testIT01CadastroConsultaEVinculo(): void
    {
        self::assertCount(0, $this->service->getEmployeeEmergencyContactList($this->empNumber));
        $id = $this->criarContato();
        $this->em->clear();
        $loaded = $this->service->getEmployeeEmergencyContact($this->empNumber, $id);
        self::assertNotNull($loaded);
        self::assertSame('Contato Ficticio T2', $loaded->getName());
        self::assertSame('Irma', $loaded->getRelationship());
        self::assertSame('51999990000', $loaded->getHomePhone());
        self::assertSame('', $loaded->getMobilePhone());
        self::assertSame('', $loaded->getOfficePhone());
        self::assertSame($this->empNumber, $loaded->getEmployee()->getEmpNumber());
        self::assertSame($id, (int) $loaded->getSeqNo());
        self::assertCount(1, $this->service->getEmployeeEmergencyContactList($this->empNumber));
    }

    public function testIT02AtualizacaoSemDuplicacao(): void
    {
        $id = $this->criarContato();
        $this->em->clear();
        $contact = $this->service->getEmployeeEmergencyContact($this->empNumber, $id);
        $contact->setHomePhone('5133330000');
        $this->service->saveEmpEmergencyContact($contact);
        $this->em->clear();
        $loaded = $this->service->getEmployeeEmergencyContact($this->empNumber, $id);
        self::assertNotNull($loaded);
        self::assertSame($id, (int) $loaded->getSeqNo());
        self::assertSame('5133330000', $loaded->getHomePhone());
        self::assertSame('Contato Ficticio T2', $loaded->getName());
        self::assertSame('Irma', $loaded->getRelationship());
        self::assertSame($this->empNumber, $loaded->getEmployee()->getEmpNumber());
        self::assertCount(1, $this->service->getEmployeeEmergencyContactList($this->empNumber));
    }
}
