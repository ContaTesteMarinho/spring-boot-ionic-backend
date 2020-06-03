package com.feliphe.cursomc.cliente.service;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.feliphe.cursomc.cidade.domain.Cidade;
import com.feliphe.cursomc.cliente.domain.Cliente;
import com.feliphe.cursomc.cliente.domain.enums.Perfil;
import com.feliphe.cursomc.cliente.domain.enums.TipoCliente;
import com.feliphe.cursomc.cliente.dto.ClienteDTO;
import com.feliphe.cursomc.cliente.dto.ClienteNewDTO;
import com.feliphe.cursomc.cliente.repository.ClienteRepository;
import com.feliphe.cursomc.endereco.domain.Endereco;
import com.feliphe.cursomc.endereco.repository.EnderecoRepository;
import com.feliphe.cursomc.exception.AuthorizationException;
import com.feliphe.cursomc.exception.DataIntegrityException;
import com.feliphe.cursomc.exception.ObjectNotFoundException;
import com.feliphe.cursomc.security.UserSS;
import com.feliphe.cursomc.user.service.UserService;
import com.feliphe.cursomc.util.service.ImageService;
import com.feliphe.cursomc.util.service.S3Service;

@Service
public class ClienteService {

	@Autowired
	private BCryptPasswordEncoder pe;
	
	@Autowired
	private ClienteRepository repo;
	
	@Autowired
	private EnderecoRepository enderecoRepository;
	
	@Autowired
	private S3Service s3Service;
	
	@Autowired
	private ImageService imageService;
	
	@Value("${img.prefix.client.profile}")
	private String prefix;
	
	@Value("${img.profile.size}")
	private Integer size;
	
	public Cliente find(Integer id) {
		
		UserSS user = UserService.authenticated();
		if((user == null || !user.hasRole(Perfil.ADMIN)) && !id.equals(user.getId())) {
			throw new AuthorizationException("Acesso negado, meu jovem.");
		}
		
		Optional<Cliente> obj = repo.findById(id);
		return obj.orElseThrow(() -> new ObjectNotFoundException(
				"Objeto não encontrado! id " + id + ", Tipo: " + Cliente.class.getName()));
	}
	
	@Transactional
	public Cliente insert(Cliente obj) {
		
		obj.setId(null);
		obj = repo.save(obj);
		enderecoRepository.saveAll(obj.getEnderecos());
		return obj;
	}
	
	public Cliente update(Cliente obj) {
		
		Cliente newObj = find(obj.getId()); 
		updateData(newObj, obj);
		return repo.save(newObj);
	}
	
	private void updateData(Cliente newObj, Cliente obj) { 
		
		newObj.setNome(obj.getNome());
		newObj.setEmail(obj.getEmail());
	}
	
	public void delete(Integer id) {
		
		find(id);
		try {
			repo.deleteById(id);
		} catch (DataIntegrityViolationException e) {
			throw new DataIntegrityException("Não é possível excluir porque há Pedidos relacionados.");
		}
	}
	
	public List<Cliente> findAll() {
		return repo.findAll();
	}
	
	public Cliente findByEmail(String email) {
		
		UserSS user = UserService.authenticated();
		
		if(user == null || !user.hasRole(Perfil.ADMIN) && !user.getUsername().equals(email)) {
			throw new AuthorizationException("Acesso negado.");
		}
		
		Cliente cli = repo.findByEmail(email);
		
		if(cli == null) {
			throw new ObjectNotFoundException("Objeto não encontrado! id: " + user.getId() + ""
					+ ", Tipo: " + Cliente.class.getName() + ", Verifique se o email foi escrito corretamente");
		}
		
		return cli;
	}
	
	public Page<Cliente> findPage(Integer page, Integer linesPerPage, String orderBy, String direction) {
		
		PageRequest pageRequest = PageRequest.of(page, linesPerPage, Direction.valueOf(direction), orderBy);
		return repo.findAll(pageRequest);
	}
	
	public Cliente fromDTO(ClienteDTO objDTO) {
		return new Cliente(objDTO.getId(), objDTO.getNome(), objDTO.getEmail(), null, null, null);
	}
	
	public Cliente fromDTO(ClienteNewDTO objDto) {
		
		Cliente cli = new Cliente(null, objDto.getNome(), objDto.getEmail(), objDto.getCpfOuCnpj(), TipoCliente.toEnum(objDto.getTipo()), pe.encode(objDto.getSenha()));
		Cidade cid = new Cidade(objDto.getCidadeId(), null, null);
		Endereco end = new Endereco(null, objDto.getLogradouro(), objDto.getNumero(), objDto.getComplemento(), objDto.getBairro(), objDto.getCep(), cli, cid);
		cli.getEnderecos().add(end);
		cli.getTelefones().add(objDto.getTelefone1());
		
		if (objDto.getTelefone2()!=null) {
			cli.getTelefones().add(objDto.getTelefone2());
		}
		if (objDto.getTelefone3()!=null) {
			cli.getTelefones().add(objDto.getTelefone3());
		}
		return cli;
	}
	
	public URI uploadProfilePicture(MultipartFile multiPartFile) {
		
		UserSS user = UserService.authenticated();
		
		if(user == null) {
			throw new AuthorizationException("Acesso negado");
		}
		
		BufferedImage jpgImage = imageService.getJpgImageFromFile(multiPartFile);
		jpgImage = imageService.cropSquare(jpgImage);
		jpgImage = imageService.resize(jpgImage, size);		
		
		String fileName = prefix + user.getId() + ".jpg";	
		
		return s3Service.uploadFile(fileName, imageService.getInputStream(jpgImage, "jpg"), "image");
	}
}