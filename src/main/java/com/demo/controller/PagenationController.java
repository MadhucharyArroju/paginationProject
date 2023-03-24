package com.demo.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.eldocomp.rrapis.constants.RRAPIConstants;
import com.eldocomp.rrapis.dto.JSONResponse;
import com.eldocomp.rrapis.dto.Pagination;
import com.eldocomp.rrapis.dto.member.coverage.DocumentUploadDTO;
import com.eldocomp.rrapis.dto.member.coverage.ResponseDocumentUploadDTOList;
import com.eldocomp.rrapis.entities.documentmanagement.DocumentUpload;
import com.eldocomp.rrapis.entities.members.Dependent;
import com.eldocomp.rrapis.entities.members.Member;
import com.eldocomp.rrapis.exception.ResourceNotFoundException;

import src.main.java.com.eldocomp.rrapis.controllers.GetMapping;
import src.main.java.com.eldocomp.rrapis.controllers.Integer;
import src.main.java.com.eldocomp.rrapis.controllers.JsonProcessingException;
import src.main.java.com.eldocomp.rrapis.controllers.NotAcceptableException;
import src.main.java.com.eldocomp.rrapis.controllers.RequestParam;
import src.main.java.com.eldocomp.rrapis.controllers.ResponseEntity;
import src.main.java.com.eldocomp.rrapis.controllers.String;
import src.main.java.com.eldocomp.rrapis.controllers.UnsupportedEncodingException;
import src.main.java.com.eldocomp.rrapis.controllers.ValidateJson;

public class PagenationController {

	public PagenationController() {
		// TODO Auto-generated constructor stub
	}
	@GetMapping("/getdocumentupload")
	public ResponseEntity<String> getDocumentUpload(
			@ValidateJson(SchemaLocations.GET_DOCUMENT_UPLOAD) @RequestParam(value = "memberoid", required = false) String memberoid,
			@RequestParam(value = "membertype", required = false) String membertype,
			@RequestParam(value = "uploadedbyoid", required = false) String uploadedbyoid,
			@RequestParam(value = "pagenumber", required = false, defaultValue = "1") Integer pagenumber,
			@RequestParam(value = "pagesize", required = false, defaultValue = "10") Integer pagesize,
			@RequestParam(value = "SortBy", required = false, defaultValue = "sentDate") String SortBy,
			@RequestParam(value = "SortDirection", required = false, defaultValue = "desc") String SortDirection,
			@RequestParam(value = "searchkey", required = false) String searchkey) throws JsonProcessingException {

		try {
			if (null != searchkey) {
				searchkey = URLDecoder.decode(searchkey, "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			logger.error("Error while encoding in search getDocumentUpload");
		}

		if (pagenumber < 1) {
			throw new NotAcceptableException("Page number can't be less than 1");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("getDocumentUpload DocumentManagementController entry:");
		}
		String response = documentManagementService.getDocumentUploadList(memberoid, membertype, uploadedbyoid,
				pagenumber, pagesize, SortBy, SortDirection, searchkey);

		if (null != response && !response.isEmpty()) {
			return new ResponseEntity(response, HttpStatus.OK);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("getDocumentUpload DocumentManagementController exit:");
			}
			return new ResponseEntity(response, HttpStatus.NOT_FOUND);
		}
	}

	
	@Override
	public String getDocumentUploadList(String memberoid, String membertype, String uploadedbyoid, Integer pagenumber,
			Integer pagesize, String sortby, String sortdirection, String searchkey) {

		List<DocumentUploadDTO> ListOfDocumentUploadObj = null;
		if (!StringUtils.isEmpty(memberoid) && !StringUtils.isEmpty(membertype))

		{
			hipaaPermissionsHelper.init(memberoid, membertype);
			hipaaPermissionsHelper.validatePermission();
		}

		Page<DocumentUpload> pagedResult = null;
		Sort sort = "desc".equalsIgnoreCase(sortdirection) ? Sort.by(sortby).descending() : Sort.by(sortby).ascending();

		Pageable paging = PageRequest.of(--pagenumber, pagesize, sort);

		pagedResult = findByCriteria(memberoid, membertype, uploadedbyoid, searchkey, paging);

		if (pagedResult.hasContent()) {
			logger.info("pagedResult::::" + pagedResult.getSize());

			List<DocumentUpload> documentUpload = pagedResult.getContent();
			ListOfDocumentUploadObj = documentUpload.stream()
					.map(documentUploadObj -> convertDocumentUplaodEntityListToDTO(documentUploadObj))
					.collect(Collectors.toList());
		} else {
			throw new ResourceNotFoundException("No Search exist for given Criteria");
		}

		Pagination pagination = new Pagination(pagedResult.getPageable().getPageSize(),
				pagedResult.getPageable().getPageNumber() + 1, pagedResult.getTotalElements());

		ResponseDocumentUploadDTOList responseDocumentUploadDTO = new ResponseDocumentUploadDTOList();
		responseDocumentUploadDTO.setPagination(pagination);
		responseDocumentUploadDTO.setListOfDocumentUploadObj(ListOfDocumentUploadObj);
		JSONResponse jsonResponse = new JSONResponse();
		jsonResponse.setResponse(responseDocumentUploadDTO);
		jsonResponse.setSchema("getdocumentupload_post.json");
		jsonResponse.setVersion("v1");
		String json = encryptionService.encryptResponse(jsonResponse);

		logger.info("getDocumentUploadList DocumentManagementServiceImpl exit");
		return json;

	}

	private DocumentUploadDTO convertDocumentUplaodEntityListToDTO(DocumentUpload documentUploadObj) {
		logger.info("convertDocumentUplaodEntityListToDTO DocumentManagementServiceImpl Enter::");
		List<DocumentUploadDTO> documentUploadDTOList = new ArrayList();
		DocumentUploadDTO documentUpload = convertEntitytoDTO(documentUploadObj);
		documentUploadDTOList.add(documentUpload);
		logger.info("convertDocumentUplaodEntityListToDTO DocumentManagementServiceImpl exit");
		return documentUpload;
	}

	private Page<DocumentUpload> findByCriteria(String memberOid, String membertype, String uploadedbyoid,
			String searchKey, Pageable paging) {
		logger.info("Enter DocumentManagementServiceImpl findbycriteria()");
		Page<DocumentUpload> page = null;
		try {
			page = documentUploadRepository.findAll(new Specification<DocumentUpload>() {
				private static final long serialVersionUID = 1432451361017384186L;

				@Override
				public Predicate toPredicate(Root<DocumentUpload> root, CriteriaQuery<?> query,
						CriteriaBuilder criteriaBuilder) {
					Predicate prMemberFn = null;
					Predicate prMemberLn = null;
					Predicate prDependentFn = null;
					Predicate prDependentLn = null;
					Predicate prMemberWSFn = null;
					Predicate prDependentWSLn = null;
					Predicate oidMemberWSQuery = null;
					Predicate oidDependentWSQuery = null;
					List<Predicate> predicates = new ArrayList<>();

					Predicate predicates1 = null;

					Predicate predicates2 = null;
					if (RRAPIConstants.USER_TYPE_EMPLOYEE.equalsIgnoreCase(membertype)) {
						Predicate Oid = criteriaBuilder.equal(root.get("memberOid"), memberOid);
						Predicate dependentOid = criteriaBuilder.isNull(root.get("dependentOid"));
						Predicate uploadedbyOid = criteriaBuilder.equal(root.get("uploadedByOid"), uploadedbyoid);

						predicates1 = criteriaBuilder.and(Oid, dependentOid);

						predicates.add(criteriaBuilder.or(predicates1, uploadedbyOid));

					}

					if (RRAPIConstants.USER_TYPE_DEPENDENT.equalsIgnoreCase(membertype)) {
						Predicate Oid = criteriaBuilder.equal(root.get("dependentOid"), memberOid);
						predicates.add(criteriaBuilder.and(Oid));

					}

					if (!StringUtils.isEmpty(searchKey) && !searchKey.contains(" ")) {

						Predicate prReferenceNumber = criteriaBuilder.like(
								criteriaBuilder.lower(root.get("referenceNumber")), searchKey.toLowerCase() + "%");

						Predicate prTitle = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")),
								searchKey.toLowerCase() + "%");

						Join<DocumentUpload, Member> joinMember = memberJoin(criteriaBuilder, root, memberOid);
						prMemberFn = criteriaBuilder.like(criteriaBuilder.lower(joinMember.get("firstName")),
								searchKey.toLowerCase() + "%");
						prMemberLn = criteriaBuilder.like(criteriaBuilder.lower(joinMember.get("lastName")),
								searchKey.toLowerCase() + "%");

						Join<DocumentUpload, Dependent> joindependednt = dependentJoin(criteriaBuilder, root,
								memberOid);

						prDependentFn = criteriaBuilder.like(criteriaBuilder.lower(joindependednt.get("firstName")),
								searchKey.toLowerCase() + "%");
						prDependentLn = criteriaBuilder.like(criteriaBuilder.lower(joindependednt.get("lastName")),
								searchKey.toLowerCase() + "%");

						predicates.add(criteriaBuilder.or(prTitle, prReferenceNumber, prMemberFn, prMemberLn,
								prDependentFn, prDependentLn));

						return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
					}

					if (!StringUtils.isEmpty(searchKey) && searchKey.contains(" ")) {

						String[] searchArray = searchKey.trim().split(" ");

						if (searchArray.length > 1) {
							String firstMemberName = searchArray[0];
							String lastMemberName = searchArray[1];

							Predicate prTitleFn = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")),
									searchKey.toLowerCase() + "%");

							Join<DocumentUpload, Member> joinMember = memberJoin(criteriaBuilder, root, memberOid);

							prMemberWSFn = criteriaBuilder.like(criteriaBuilder.lower(joinMember.get("firstName")),
									firstMemberName.toLowerCase() + "%");
							prMemberLn = criteriaBuilder.like(criteriaBuilder.lower(joinMember.get("lastName")),
									lastMemberName.toLowerCase() + "%");

							oidMemberWSQuery = criteriaBuilder.and(prMemberWSFn, prMemberLn);

							Join<DocumentUpload, Dependent> joindependednt = dependentJoin(criteriaBuilder, root,
									memberOid);

							prDependentFn = criteriaBuilder.like(criteriaBuilder.lower(joindependednt.get("firstName")),
									firstMemberName.toLowerCase() + "%");

							prDependentWSLn = criteriaBuilder.like(
									criteriaBuilder.lower(joindependednt.get("lastName")),
									lastMemberName.toLowerCase() + "%");

							oidDependentWSQuery = criteriaBuilder.and(prDependentFn, prDependentWSLn);

							predicates.add(criteriaBuilder.or(prTitleFn, oidMemberWSQuery, oidDependentWSQuery));

						}
					}

					return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));

				}

				private Join<DocumentUpload, Dependent> dependentJoin(CriteriaBuilder criteriaBuilder,
						Root<DocumentUpload> root, String pvDependentOid) {

					logger.debug("Enter DocumentManagementServiceImpl  dependentJoin()");
					Join<DocumentUpload, Dependent> joindependednt = root.join("dependent", JoinType.LEFT);
					Predicate pBlob = criteriaBuilder.equal(joindependednt.get("oid"), pvDependentOid);
					logger.debug("Exit DocumentManagementServiceImpl  dependentJoin()");
					return joindependednt;
				}

				private Join<DocumentUpload, Member> memberJoin(CriteriaBuilder criteriaBuilder,
						Root<DocumentUpload> root, String memberOid) {
					logger.debug("Enter DocumentManagementServiceImpl  memberJoin()");
					Join<DocumentUpload, Member> joinMember = root.join("member", JoinType.INNER);
					Predicate pBlobMemberOid = criteriaBuilder.equal(joinMember.get("oid"), memberOid);
					logger.debug("Exit DocumentManagementServiceImpl  memberJoin()");
					return joinMember;
				}

			}, paging);
			return page;
		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.info("Exit DocumentManagementServiceImpl findbycriteria()");
		return page;

	}
	

@Repository
public interface DocumentUploadRepository
		extends PagingAndSortingRepository<DocumentUpload, String>, JpaSpecificationExecutor<DocumentUpload> {

	List<DocumentUpload> findAll();

	DocumentUpload findByOid(String Oid);

}
}
