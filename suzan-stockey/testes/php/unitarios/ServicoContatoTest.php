<?php
namespace Suzan\EmergencyContacts;

use OrangeHRM\Entity\Employee;
use OrangeHRM\Entity\EmpEmergencyContact;
use OrangeHRM\Pim\Dao\EmpEmergencyContactDao;
use OrangeHRM\Pim\Service\EmpEmergencyContactService;
use PHPUnit\Framework\TestCase;

class ServicoContatoTest extends TestCase
{
    public function testUT04DelegacaoSemBanco(): void
    {
        $employee = new Employee();
        $employee->setEmpNumber(1);
        $contact = new EmpEmergencyContact();
        $contact->setEmployee($employee);
        $contact->setName('Contato Ficticio T2');
        $contact->setRelationship('Irma');
        $contact->setHomePhone('51999990000');
        $dao = $this->createMock(EmpEmergencyContactDao::class);
        $dao->expects(self::once())->method('saveEmployeeEmergencyContact')
            ->with(self::identicalTo($contact))->willReturn($contact);
        $service = new EmpEmergencyContactService();
        $service->setEmpEmergencyContactDao($dao);
        self::assertSame($contact, $service->saveEmpEmergencyContact($contact));
    }
}
