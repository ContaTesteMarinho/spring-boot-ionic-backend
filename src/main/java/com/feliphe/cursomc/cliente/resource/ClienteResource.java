package com.feliphe.cursomc.cliente.resource;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.feliphe.cursomc.cliente.domain.Cliente;
import com.feliphe.cursomc.cliente.dto.ClienteDTO;
import com.feliphe.cursomc.cliente.dto.ClienteNewDTO;
import com.feliphe.cursomc.cliente.service.ClienteService;
import com.feliphe.cursomc.pedido.domain.Pedido;
import com.feliphe.cursomc.pedido.service.PedidoService;

@RestController
@RequestMapping(value="/clientes")
public class ClienteResource {

	@Autowired
	ClienteService service;
	
	@Autowired
	PedidoService pedidoService;
	
	@RequestMapping(value="/{id}", method=RequestMethod.GET)
	public ResponseEntity<?> buscar(@PathVariable Integer id) {
		
		Cliente obj = service.find(id);
		return ResponseEntity.ok().body(obj);
	}
	
	@RequestMapping(value = "/{clienteId}/pedidos", method = RequestMethod.GET)
	public ResponseEntity<List<Pedido>> listByCliente(@PathVariable Integer clienteId, Pageable pageable) {

		List<Pedido> entities = pedidoService.listByCliente(clienteId, pageable);
		return ResponseEntity.ok().body(entities);
	}

	
	@RequestMapping(value="/email", method=RequestMethod.GET)
	public ResponseEntity<?> findByEmail(@RequestParam(value="value") String email) {
		
		Cliente obj = service.findByEmail(email);
		return ResponseEntity.ok().body(obj);
	}
	
	@RequestMapping(method=RequestMethod.POST)
	public ResponseEntity<Void> insert(@Valid @RequestBody ClienteNewDTO objDTO) {
		
		Cliente obj = service.fromDTO(objDTO);
		obj = service.insert(obj);
		
		URI uri = ServletUriComponentsBuilder
									.fromCurrentRequestUri()
									.path("/{id}")
									.buildAndExpand(obj.getId())
									.toUri();
		
		return ResponseEntity.created(uri).build();
	}
	
	@RequestMapping(value="/{id}", method=RequestMethod.PUT)
	public ResponseEntity<Void> update(@Valid @RequestBody ClienteDTO objDTO, @PathVariable Integer id) {
		
		Cliente obj = service.fromDTO(objDTO);
		obj.setId(id);
		obj = service.update(obj);
		
		return ResponseEntity.noContent().build();
	}
	 
	@PreAuthorize("hasAnyRole('ADMIN')")
	@RequestMapping(value="/{id}", method=RequestMethod.DELETE)
	public ResponseEntity<Void> delete(@PathVariable Integer id) {
		
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
	
	@PreAuthorize("hasAnyRole('ADMIN')")
	@RequestMapping(method=RequestMethod.GET)
	public ResponseEntity<List<ClienteDTO>> findAll() {
		
		List<Cliente> categorias = service.findAll();
		List<ClienteDTO> categoriasDTO = categorias
												.stream()
												.map(categoria -> new ClienteDTO(categoria))
												.collect(Collectors.toList());
		
		return ResponseEntity.ok().body(categoriasDTO);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN')")
	@RequestMapping(value="/page", method=RequestMethod.GET)
	public ResponseEntity<Page<ClienteDTO>> findPage(
			@RequestParam(value="page", defaultValue="0") Integer page,
			@RequestParam(value="linesPerPage", defaultValue="24") Integer linesPerPage,
			@RequestParam(value="orderBy", defaultValue="nome") String orderBy,
			@RequestParam(value="direction", defaultValue="ASC") String direction) {
		
		Page<Cliente> categorias = service.findPage(page, linesPerPage, orderBy, direction);
		Page<ClienteDTO> categoriasDTO = categorias.map(categoria -> new ClienteDTO(categoria));
		
		return ResponseEntity.ok().body(categoriasDTO);
	}
	
	@RequestMapping(value="/picture", method=RequestMethod.POST)
	public ResponseEntity<Void> insert(
			@RequestParam(name="file") MultipartFile multiPartFile) {
		
		URI uri = service.uploadProfilePicture(multiPartFile);		
		return ResponseEntity.created(uri).build();
	}
	
	/*
	 * @GetMapping("/{clientId}/pedidos") public ResponseEntity<Page<Pedido>>
	 * pedidosCliente(@PathVariable Integer clientId,
	 * 
	 * @RequestParam(value = "page", defaultValue = "0") Integer page,
	 * 
	 * @RequestParam(value = "linesPerPage", defaultValue = "24") Integer
	 * linesPerPage,
	 * 
	 * @RequestParam(value = "orderBy", defaultValue = "instante") String orderBy,
	 * 
	 * @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
	 * 
	 * Page<Pedido> pedidos = service.findPage(page, linesPerPage, orderBy,
	 * direction); return ResponseEntity.ok().body(pedidos); }
	 */
}